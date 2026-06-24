package com.example.shop.service.impl;


import com.example.shop.config.JsonUtil;

import com.example.shop.entity.RestaurantTable;
import com.example.shop.entity.TableBooking;
import com.example.shop.entity.enums.ActivityLog;
import com.example.shop.entity.enums.BookingStatus;
import com.example.shop.entity.enums.CancelReason;
import com.example.shop.entity.enums.TableStatus;
import com.example.shop.exception.APIException;

import com.example.shop.hellper.SecuritySnapshotUtil;

import com.example.shop.payloads.BookingStatusDTO;

import com.example.shop.payloads.BookingStatusSummaryDTO;
import com.example.shop.payloads.TableBookingRequestDTO;
import com.example.shop.payloads.TableBookingResponseDTO;
import com.example.shop.repository.ActivityRepo;
import com.example.shop.repository.RestaurantRepo;
import com.example.shop.repository.TableBookingRepo;
import com.example.shop.service.ActivityLogService;
import com.example.shop.service.TableBookingService;
import com.example.shop.hellper.TextNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
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

        TableBooking booking = modelMapper.map(request, TableBooking.class);
        booking.setTable(table);
        booking.setStatus(BookingStatus.PENDING);
        booking.setCustomerName(
                TextNormalizer.normalizeName(request.getCustomerName())
        );

        TableBooking saved = tableBookingRepo.save(booking);

        String newSnapshot=jsonUtil.toJson(saved);
        activityLogService.log("BOOKING",saved.getId(),ActivityLog.CREATE_BOOKING.toString(),null,newSnapshot
                ,saved.getCustomerName(),saved.getCustomerPhone(),"create booking");

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


        String oldSnapshot=jsonUtil.toJson(booking);
        // 3. Update trạng thái + audit
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdateBy(SecuritySnapshotUtil.getUserName());
        booking.setUpdatedRole(SecuritySnapshotUtil.getRole());
        booking.setUpdateAt(LocalDateTime.now());

        TableBooking savedBooking = tableBookingRepo.save(booking);

        String newSnapshot=jsonUtil.toJson(savedBooking);

        activityLogService.log("BOOKING",savedBooking.getId(), ActivityLog.CONFIRM_BOOKING.toString(),oldSnapshot,newSnapshot
                ,savedBooking.getCustomerName(),savedBooking.getCustomerPhone(),"confirm booking");
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
        booking.setUpdateBy(SecuritySnapshotUtil.getUserName()); //TODO: SMAPSHORT
        booking.setUpdatedRole(role);
        booking.setUpdateAt(LocalDateTime.now());

        TableBooking savedBooking = tableBookingRepo.save(booking);
        String newSnapShort=jsonUtil.toJson(savedBooking);

        activityLogService.log("BOOKING",savedBooking.getId(),ActivityLog.CANCEL_BOOKING.toString(),oldSnapshot,newSnapShort
        ,savedBooking.getCustomerName(),savedBooking.getCustomerPhone(),"Cancel booking");


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

        String oldSnapshot=jsonUtil.toJson(booking);


        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.getTable().setStatus(TableStatus.OCCUPIED);
        booking.setUpdateBy(SecuritySnapshotUtil.getUserName());
        booking.setUpdatedRole(SecuritySnapshotUtil.getRole());
        booking.setUpdateAt(LocalDateTime.now());

        TableBooking savedBooking = tableBookingRepo.save(booking);

        String newSnapshot=jsonUtil.toJson(savedBooking);

        activityLogService.log("BOOKING",savedBooking.getId(),ActivityLog.CHECK_IN_BOOKING.toString(),oldSnapshot,newSnapshot
                ,savedBooking.getCustomerName(),savedBooking.getCustomerPhone(),"Check in booking");

        TableBookingResponseDTO response =
                modelMapper.map(savedBooking, TableBookingResponseDTO.class);

        response.setBookingId(savedBooking.getId());
        response.setTableName(savedBooking.getTable().getTableName());
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
        String oldSnapshot=jsonUtil.toJson(booking);

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new APIException("Chỉ booking đã check-in mới được hoàn thành");
        }


        booking.setStatus(BookingStatus.COMPLETED);
        booking.setUpdateAt(LocalDateTime.now());
        booking.setUpdateBy(SecuritySnapshotUtil.getUserName());
        booking.setUpdatedRole(SecuritySnapshotUtil.getRole());
        booking.getTable().setStatus(TableStatus.AVAILABLE);

        TableBooking savedBooking = tableBookingRepo.save(booking);
        String newSnapshot=jsonUtil.toJson(savedBooking);

        activityLogService.log("BOOKING",savedBooking.getId(),ActivityLog.AVAILABLE_BOOKING.toString(),oldSnapshot,newSnapshot
                ,savedBooking.getCustomerName(),savedBooking.getCustomerPhone(),"Check out booking");

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
        String oldSnapshot=jsonUtil.toJson(expiredBookings);


        for (TableBooking booking : expiredBookings) {

            booking.setStatus(BookingStatus.NO_SHOW);
            booking.getTable().setStatus(TableStatus.AVAILABLE);

            booking.setUpdateBy("SYSTEM");
            booking.setUpdatedRole("SYSTEM");
            booking.setUpdateAt(LocalDateTime.now());

            TableBooking savedBooking = tableBookingRepo.save(booking);

            String newSnapshot=jsonUtil.toJson(savedBooking);


            activityLogService.log("BOOKING",savedBooking.getId(),ActivityLog.NO_SHOW_BOOKING.toString(),oldSnapshot,newSnapshot
                    ,savedBooking.getCustomerName(),savedBooking.getCustomerPhone(),"Not show booking");

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
    public List<BookingStatusSummaryDTO>getBookingByStatus(){
        //PENDING   → ?
        Map<BookingStatus,Long>summary=new EnumMap<>(BookingStatus.class);
        //NO_SHOW → 0
        for (BookingStatus status:BookingStatus.values()){
            summary.put(status,0L);
        }

        for(Object[] row:tableBookingRepo.getBookingGroupByStatus()){
            BookingStatus status=(BookingStatus) row[0];
            Long total=(Long) row[1];
            summary.put(status,total);
        }

        return summary.entrySet().stream()
                .map(e->new BookingStatusSummaryDTO(e.getKey(),e.getValue())).toList();
    }
}
