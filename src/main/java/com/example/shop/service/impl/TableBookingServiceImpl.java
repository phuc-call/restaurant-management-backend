package com.example.shop.service.impl;


import com.example.shop.config.JsonUtil;

import com.example.shop.entity.RestaurantTable;
import com.example.shop.entity.TableBooking;
import com.example.shop.entity.User;
import com.example.shop.entity.enums.*;
import com.example.shop.exception.APIException;

import com.example.shop.hellper.SecuritySnapshotUtil;

import com.example.shop.hellper.StaffStatusValidator;
import com.example.shop.payloads.BookingStatusDTO;

import com.example.shop.payloads.BookingStatusSummaryDTO;
import com.example.shop.payloads.TableBookingRequestDTO;
import com.example.shop.payloads.TableBookingResponseDTO;
import com.example.shop.repository.ActivityRepo;
import com.example.shop.repository.RestaurantRepo;
import com.example.shop.repository.TableBookingRepo;
import com.example.shop.repository.UserRepo;
import com.example.shop.service.ActivityLogService;
import com.example.shop.service.TableBookingService;
import com.example.shop.hellper.TextNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.modelmapper.ModelMapper;
import org.modelmapper.internal.bytebuddy.implementation.bytecode.Throw;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;


@Service
@Transactional
public class TableBookingServiceImpl implements TableBookingService {

    @Autowired
    TableBookingRepo tableBookingRepo;
    @Autowired
    RestaurantRepo restaurantRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    SimpMessagingTemplate messagingTemplate;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    ActivityRepo activityRepo;
    @Autowired
    ActivityLogService activityLogService;
    @Autowired
    JsonUtil jsonUtil;
    @PersistenceContext
    EntityManager entityManager;
    @Autowired
    UserRepo userRepo;
    @Autowired
    private StaffStatusValidator staffStatusValidator;



    private static final int GRACE_MINUTES = 15;

    @Override
    @Transactional
    public TableBookingResponseDTO createBooking(TableBookingRequestDTO request) {

        LocalDateTime now = LocalDateTime.now();
        int MAX_BOOKING_DAYS = 3;

        if (request.getBookingTime().isBefore(now)) {
            throw new APIException("Thời gian đặt không được nhỏ hơn thời gian hiện tại");
        }

        if (request.getBookingTime().isAfter(now.plusDays(MAX_BOOKING_DAYS))) {
            throw new APIException(
                    "Chỉ được đặt bàn trong vòng " + MAX_BOOKING_DAYS + " ngày tới"
            );
        }
        if(request.getNumberOfGuests()>100){
            throw new APIException("Số lượng khách không hợp lệ");
        }
        if(request.getCustomerName().length()>30){
            throw new APIException("Tên khách hàng không hợp lệ");
        }
        List<RestaurantTable>tables;



        // hung giờ trùng ±1 tiếng
        LocalDateTime fromTime = request.getBookingTime().minusHours(1);
        LocalDateTime toTime = request.getBookingTime().plusHours(1);

        RestaurantTable table = restaurantRepo
                .findAvailableTables(
                        request.getNumberOfGuests(),
                        fromTime,
                        toTime
                )
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new APIException(
                                "Rất tiếc, đã hết bàn trong khung giờ này. Vui lòng chọn thời gian khác."
                        )
                );
        if (tableBookingRepo.existsByCustomerPhone(request.getCustomerPhone())) {
            throw new APIException("Số diện thoại đã được đăng ký, nếu quý khách có thể gặp nhân viên tại quầy thu ngân");
        }
        TableBooking booking = modelMapper.map(request, TableBooking.class);
        booking.setTable(table);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCustomerName(
                TextNormalizer.normalizeName(request.getCustomerName())
        );

        TableBooking saved = tableBookingRepo.save(booking);

        String newSnapshot = jsonUtil.toJson(saved);
        activityLogService.log(
                "BOOKING",
                saved.getId(),
                "CREATE_BOOKING",
                null,
                newSnapshot,
                "CUSTOMER",
                EActivityResult.SUCCESS,
                saved.getCustomerPhone(),
                "Khách hàng đặt bàn"
        );


        TableBookingResponseDTO response =
                modelMapper.map(saved, TableBookingResponseDTO.class);

        response.setBookingId(saved.getId());
        response.setTableName(table.getTableName());
        response.setSeatCount(table.getSeatCount().intValue());

        messagingTemplate.convertAndSend(
                "/topic/booking",
                Map.of(
                        "event", "BOOKING_CREATED",
                        "data", response
                )
        );
        return response;
    }


    @Override
    public TableBookingResponseDTO confirmBooking(Long bookingId) {

        TableBooking booking = tableBookingRepo.findById(bookingId)
                .orElseThrow(() -> new APIException("Booking không tồn tại"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new APIException("Không hợp lệ, trạng thái phải là Pending");
        }


        String oldSnapshot = jsonUtil.toJson(booking);
        // 3. Update trạng thái + audit
        booking.setStatus(BookingStatus.CONFIRMED);
        Long userId = SecuritySnapshotUtil.getUserId();
        if (userId != null) {
            User userRef = entityManager.getReference(User.class, userId);
            booking.setUpdatedByUser(userRef);
        }

        booking.setUpdatedRole(SecuritySnapshotUtil.getRole());
        booking.setUpdatedAt(LocalDateTime.now());

        TableBooking savedBooking = tableBookingRepo.save(booking);

        String newSnapshot = jsonUtil.toJson(savedBooking);

        activityLogService.log(
                "BOOKING",
                savedBooking.getId(),
                "CONFIRM_BOOKING",
                oldSnapshot,
                newSnapshot,
                SecuritySnapshotUtil.getEmployeeName(),
                EActivityResult.SUCCESS,
                savedBooking.getCustomerPhone(),
                "Nhân viên xác nhận booking cho khách "
                        + savedBooking.getCustomerName()
        );


        TableBookingResponseDTO response =
                modelMapper.map(savedBooking, TableBookingResponseDTO.class);

        response.setBookingId(savedBooking.getId());
        response.setTableName(savedBooking.getTable().getTableName());
        response.setSeatCount(savedBooking.getTable().getSeatCount().intValue());

        // 7. Websocket notify
        messagingTemplate.convertAndSend(
                "/topic/booking",
                Map.of(
                        "event", "BOOKING_CONFIRMED",
                        "data", response
                )
        );

        return response;
    }


    @Override
    public TableBookingResponseDTO cancelBooking(Long bookingId, CancelReason cancelReason) {

        String role = SecuritySnapshotUtil.getRole();
        TableBooking booking = tableBookingRepo.findById(bookingId)
                .orElseThrow(() -> new APIException("Booking không tồn tại"));
        if (role == null) {
            throw new APIException("Bạn cần đăng nhập để sử dụng chức năng này");
        }
        String oldSnapshot = jsonUtil.toJson(booking);

        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.COMPLETED
                || booking.getStatus() == BookingStatus.CHECKED_IN
                || booking.getStatus() == BookingStatus.NO_SHOW) {
            throw new APIException("Booking không thể hủy ở trạng thái hiện tại");
        }

        if ("STAFF".equals(SecuritySnapshotUtil.getRole())) {
            if (!EnumSet.of(
                    CancelReason.CUSTOMER_LATE,
                    CancelReason.CUSTOMER_REQUEST,
                    CancelReason.CUSTOMER_NO_SHOW).contains(cancelReason)) {
                throw new APIException("Nhân viên không có chức năng hủy này");
            }
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setReason(cancelReason);
        Long userId = SecuritySnapshotUtil.getUserId();
        if (userId != null) {
            User userRef = entityManager.getReference(User.class, userId);
            booking.setUpdatedByUser(userRef);
        }
        booking.setUpdatedRole(role);
        booking.setUpdatedAt(LocalDateTime.now());

        TableBooking savedBooking = tableBookingRepo.save(booking);
        String newSnapShort = jsonUtil.toJson(savedBooking);

        activityLogService.log(
                "BOOKING",
                savedBooking.getId(),
                "CANCEL_BOOKING",
                oldSnapshot,
                newSnapShort,
                SecuritySnapshotUtil.getEmployeeName(),
                EActivityResult.SUCCESS,
                savedBooking.getCustomerPhone(),        // khách liên quan
                "Hủy booking - lý do: " + cancelReason.name()
        );


        ///Map Entity → ResponseDTO
        TableBookingResponseDTO response =
                modelMapper.map(savedBooking, TableBookingResponseDTO.class);
        response.setBookingId(savedBooking.getId());
        response.setTableName(savedBooking.getTable().getTableName());
        response.setSeatCount(savedBooking.getTable().getSeatCount().intValue());

        messagingTemplate.convertAndSend(
                "/topic/booking",
                Map.of(
                        "event", "BOOKING_CANCELLED",
                        "data", response
                )
        );
        return response;
    }

    @Override
    public TableBookingResponseDTO checkIn(Long bookingId) {


        TableBooking booking = tableBookingRepo.findById(bookingId)
                .orElseThrow(() -> new APIException("Booking không tồn tại"));
        TableStatus tableStatus = booking.getTable().getStatus();
        if (tableStatus != TableStatus.AVAILABLE) {
            throw new APIException("Bàn không ở trạng thái AVAILABLE");
        }

        String oldSnapshot = jsonUtil.toJson(booking);


        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.getTable().setStatus(TableStatus.OCCUPIED);
        Long userId = SecuritySnapshotUtil.getUserId();
        if (userId != null) {
            User userRef = entityManager.getReference(User.class, userId);
            booking.setUpdatedByUser(userRef);
        }
        booking.setUpdatedRole(SecuritySnapshotUtil.getRole());
        booking.setUpdatedAt(LocalDateTime.now());

        TableBooking savedBooking = tableBookingRepo.save(booking);

        String newSnapshot = jsonUtil.toJson(savedBooking);

        activityLogService.log(
                "BOOKING",
                savedBooking.getId(),
                "CHECK_IN_BOOKING",
                oldSnapshot,
                newSnapshot,
                SecuritySnapshotUtil.getEmployeeName(),
                EActivityResult.SUCCESS,
                savedBooking.getCustomerPhone(),
                "Nhân viên check-in booking cho khách "
                        + savedBooking.getCustomerName()
        );


        TableBookingResponseDTO response =
                modelMapper.map(savedBooking, TableBookingResponseDTO.class);

        response.setBookingId(savedBooking.getId());
        response.setTableName(savedBooking.getTable().getTableName());
        response.setTableStatus(booking.getTable().getStatus());
        response.setSeatCount(savedBooking.getTable().getSeatCount().intValue());

        messagingTemplate.convertAndSend(
                "/topic/booking",
                Map.of(
                        "event", "BOOKING_CHECKED_IN",
                        "data", response
                )
        );

        return response;
    }


    @Override
    public TableBookingResponseDTO completeBooking(Long bookingId) {

        TableBooking booking = tableBookingRepo.findById(bookingId)
                .orElseThrow(() -> new APIException("Booking không tồn tại"));
        String oldSnapshot = jsonUtil.toJson(booking);

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new APIException("Chỉ booking đã check-in mới được hoàn thành");
        }
        LocalDateTime bookingTime = booking.getBookingTime();
        LocalDateTime allowBooking = bookingTime.plusMinutes(30);

        if (LocalDateTime.now().isBefore(allowBooking)) {
            throw new APIException(
                    "Chỉ được hoàn thành sau " + allowBooking
            );
        }


        booking.setStatus(BookingStatus.COMPLETED);
        booking.setUpdatedAt(LocalDateTime.now());
        Long userId = SecuritySnapshotUtil.getUserId();
        if (userId != null) {
            User userRef = entityManager.getReference(User.class, userId);
            booking.setUpdatedByUser(userRef);
        }
        booking.setUpdatedRole(SecuritySnapshotUtil.getRole());
        booking.getTable().setStatus(TableStatus.AVAILABLE);

        TableBooking savedBooking = tableBookingRepo.save(booking);
        String newSnapshot = jsonUtil.toJson(savedBooking);

        activityLogService.log(
                "BOOKING",
                savedBooking.getId(),
                "COMPLETE_BOOKING",
                oldSnapshot,
                newSnapshot,
                SecuritySnapshotUtil.getEmployeeName(),
                EActivityResult.SUCCESS,
                savedBooking.getCustomerPhone(),
                "Hoàn thành booking cho khách " + savedBooking.getCustomerName()
        );

        TableBookingResponseDTO response =
                modelMapper.map(savedBooking, TableBookingResponseDTO.class);

        response.setBookingId(savedBooking.getId());
        response.setTableName(savedBooking.getTable().getTableName());
        response.setSeatCount(savedBooking.getTable().getSeatCount().intValue());

        messagingTemplate.convertAndSend(
                "/topic/booking",
                Map.of(
                        "event", "BOOKING_COMPLETED",
                        "data", response
                )
        );

        return response;
    }


    //TODO:Watch again
    @Scheduled(fixedRate = 60_000)
    @Override
    public void autoMarkNoShow() {

        LocalDateTime expiredTime =
                LocalDateTime.now().minusMinutes(GRACE_MINUTES);

        List<TableBooking> expiredBookings =
                tableBookingRepo.findExpiredConfirmedBookings(expiredTime);
        String oldSnapshot = jsonUtil.toJson(expiredBookings);


        for (TableBooking booking : expiredBookings) {

            booking.setStatus(BookingStatus.NO_SHOW);
            booking.getTable().setStatus(TableStatus.AVAILABLE);

            Long userId = SecuritySnapshotUtil.getUserId();
            if (userId != null) {
                User userRef = entityManager.getReference(User.class, userId);
                booking.setUpdatedByUser(userRef);
            }
            booking.setUpdatedRole("SYSTEM");
            booking.setUpdatedAt(LocalDateTime.now());

            TableBooking savedBooking = tableBookingRepo.save(booking);

            String newSnapshot = jsonUtil.toJson(savedBooking);


            activityLogService.log(
                    "BOOKING",
                    savedBooking.getId(),
                    "AUTO_BOOKING",
                    oldSnapshot,
                    newSnapshot,
                    SecuritySnapshotUtil.getEmployeeName(),
                    EActivityResult.FAILED,
                    savedBooking.getCustomerPhone(),
                    "Không thấy khách " + savedBooking.getCustomerName()
            );

            TableBookingResponseDTO dto = new TableBookingResponseDTO();
            dto.setBookingId(savedBooking.getId());
            dto.setTableName(savedBooking.getTable().getTableName());
            dto.setCustomerName(savedBooking.getCustomerName());
            dto.setBookingTime(savedBooking.getBookingTime());
            dto.setStatus(BookingStatus.NO_SHOW);

            messagingTemplate.convertAndSend(
                    "/topic/booking",
                    Map.of(
                            "event", "BOOKING_NO_SHOW_AUTO",
                            "data", dto
                    )
            );
        }
    }


    @Override
    public TableBookingResponseDTO getBookingById(Long bookingId) {

        TableBooking booking = tableBookingRepo.findById(bookingId)
                .orElseThrow(() -> new APIException("Booking không tồn tại"));

        TableBookingResponseDTO response =
                modelMapper.map(booking, TableBookingResponseDTO.class);

        response.setBookingId(booking.getId());
        response.setTableName(booking.getTable().getTableName());
        response.setSeatCount(booking.getTable().getSeatCount().intValue());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TableBookingResponseDTO> getAllBookings(
            Integer pageNumber,
            Integer pageSize,
            Integer minGuests,
            Integer maxGuests,
            LocalDate fromDate,
            LocalDate toDate,
            String customerPhone,
            String customerName
    ) {
        Pageable pageable = PageRequest.of(
                pageNumber != null ? pageNumber : 0,
                pageSize != null ? pageSize : 20
        );

        LocalDateTime fromTime = null;
        LocalDateTime toTime = null;

        if (fromDate != null) {
            fromTime = fromDate.atStartOfDay();
        }
        if (toDate != null) {
            toTime = toDate.atTime(23, 59, 59);
        }

        Page<TableBooking> pageData =
                tableBookingRepo.searchBookings(
                        minGuests,
                        maxGuests,
                        fromTime,
                        toTime,
                        customerPhone,
                        customerName,
                        pageable
                );

        return pageData.map(booking -> {
            TableBookingResponseDTO dto =
                    modelMapper.map(booking, TableBookingResponseDTO.class);
            dto.setBookingId(booking.getId());
            dto.setTableName(booking.getTable().getTableName());
            dto.setSeatCount(booking.getTable().getSeatCount().intValue());
            return dto;
        });
    }


    @Override
    @Transactional(readOnly = true)
    public List<TableBookingResponseDTO> getBookingsByStatus(BookingStatus status) {
        List<TableBooking> tableBooking = tableBookingRepo.findByStatusOrderByBookingTimeAsc(status);
        return tableBooking.stream().map(booking -> {
            TableBookingResponseDTO dto =
                    modelMapper.map(booking, TableBookingResponseDTO.class);

            dto.setBookingId(booking.getId());
            dto.setTableName(booking.getTable().getTableName());
            dto.setSeatCount(booking.getTable().getSeatCount().intValue());

            return dto;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableBookingResponseDTO> getBookingsByTable(String tableName) {

        List<TableBooking> bookings =
                tableBookingRepo.findByTable_TableNameOrderByBookingTimeAsc(tableName);

        return bookings.stream().map(booking -> {
            TableBookingResponseDTO dto =
                    modelMapper.map(booking, TableBookingResponseDTO.class);

            dto.setBookingId(booking.getId());
            dto.setTableName(booking.getTable().getTableName());
            dto.setSeatCount(booking.getTable().getSeatCount().intValue());

            return dto;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTimeSlotAvailable(Long tableId, LocalDateTime bookingTime) {

        return !tableBookingRepo.existsByTable_TableIdAndStatusInAndBookingTimeBetween(
                tableId,
                List.of(
                        BookingStatus.PENDING,
                        BookingStatus.CONFIRMED,
                        BookingStatus.CHECKED_IN
                ),
                bookingTime.minusHours(1),
                bookingTime.plusHours(1)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> suggestTableIds(
            int numberOfGuests,
            LocalDateTime bookingTime
    ) {

        return restaurantRepo
                .findAvailableTablesForBooking(
                        (long) numberOfGuests,
                        bookingTime
                )
                .stream()
                .map(RestaurantTable::getTableId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableBookingResponseDTO> getBookingsByCustomerPhone(String phone) {
        List<TableBooking> tableBooking = tableBookingRepo.findByCustomerPhoneOrderByBookingTimeDesc(phone);
        if (tableBooking == null) {
            throw new APIException("Không tìm thấy bàn với số " + phone);
        }
        return tableBooking.stream().map(tableBooking1 -> {
                    TableBookingResponseDTO dto =
                            modelMapper.map(tableBooking1, TableBookingResponseDTO.class);
                    dto.setBookingId(tableBooking1.getId());
                    dto.setTableName(tableBooking1.getTable().getTableName());
                    dto.setSeatCount(tableBooking1.getTable().getSeatCount().intValue());
                    return dto;
                }

        ).toList();
    }

    @Override
    public List<BookingStatusDTO> getBookingStatus() {
        return tableBookingRepo.findAllStaus()
                .stream().map(BookingStatusDTO::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingStatusSummaryDTO> getBookingByStatus() {
        //PENDING   → ?
        Map<BookingStatus, Long> summary = new EnumMap<>(BookingStatus.class);
        //NO_SHOW → 0
        for (BookingStatus status : BookingStatus.values()) {
            summary.put(status, 0L);
        }

        for (Object[] row : tableBookingRepo.getBookingGroupByStatus()) {
            BookingStatus status = (BookingStatus) row[0];
            Long total = (Long) row[1];
            summary.put(status, total);
        }

        return summary.entrySet().stream()
                .map(e -> new BookingStatusSummaryDTO(e.getKey(), e.getValue())).toList();
    }

    @Override
    public void changeTableForBooking(Long bookingId, Long newTableId) {

        // 1. Lấy booking cần đổi
        TableBooking booking = tableBookingRepo.findById(bookingId)
                .orElseThrow(() -> new APIException("Không tìm thấy booking"));
        Set<BookingStatus> INVALID_STATUSES = EnumSet.of(
                BookingStatus.CANCELLED,
                BookingStatus.NO_SHOW,
                BookingStatus.COMPLETED
        );

        if (INVALID_STATUSES.contains(booking.getStatus())) {
            throw new APIException(
                    "Không thể đổi bàn với booking ở trạng thái: " + booking.getStatus()
            );
        }


        // 2. Lấy bàn mới
        RestaurantTable newTable = restaurantRepo.findById(newTableId)
                .orElseThrow(() -> new APIException("Không tìm thấy bàn"));

        // 3. Chỉ cho đổi nếu bàn mới AVAILABLE
        if (newTable.getStatus() != TableStatus.AVAILABLE) {
            throw new APIException("Tạm thời chưa có bàn trống");
        }

        // 4. Đổi bàn cho booking
        booking.setTable(newTable);

        tableBookingRepo.save(booking);

        messagingTemplate.convertAndSend(
                "/topic/booking",
                Map.of(
                        "event", "BOOKING_TABLE_CHANGED",
                        "data", Map.of(
                                "bookingId", booking.getId(),
                                "tableId", newTable.getTableId(),
                                "tableName", newTable.getTableName(),
                                "tableStatus", newTable.getStatus().name()
                        )
                )
        );
    }
}
