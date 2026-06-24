package com.example.shop.service.impl;

import com.example.shop.entity.ShiftGroup;
import com.example.shop.entity.User;
import com.example.shop.entity.WorkShift;

import com.example.shop.entity.enums.EAccountStatus;
import com.example.shop.entity.enums.ShiftType;
import com.example.shop.exception.APIException;
import com.example.shop.hellper.SecuritySnapshotUtil;
import com.example.shop.payloads.AssignShiftRequestDTO;
import com.example.shop.payloads.BulkAssignShiftRequestDTO;
import com.example.shop.payloads.Event.CreateShiftGroupRequest;
import com.example.shop.payloads.WorkShiftDTO;
import com.example.shop.payloads.reponse.EmployeeScheduleResponse;
import com.example.shop.payloads.reponse.ShiftStatisticResponse;
import com.example.shop.payloads.reponse.UserShiftDTO;
import com.example.shop.payloads.reponse.WorkShiftOverview;
import com.example.shop.repository.ShiftGroupRepo;
import com.example.shop.repository.UserRepo;
import com.example.shop.repository.WorkShiftRepo;
import com.example.shop.service.WorkShiftService;
import lombok.RequiredArgsConstructor;

import lombok.extern.java.Log;
import org.apache.xmlbeans.impl.xb.xsdschema.Attribute;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkShiftServiceImpl implements WorkShiftService {
    private final WorkShiftRepo workShiftRepo;
    private final UserRepo userRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final ModelMapper modelMapper;
    private final ShiftGroupRepo shiftGroupRepo;

    //websocket
    private void webSocket(String event, Object data) {
        messagingTemplate.convertAndSend(
                "/topic/work-shift",
                Map.of(
                        "event", event,
                        "data", data
                )
        );
    }

    //validation account and user
    private User getAccountUserAndStatusUser(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new APIException("User not found"));
        if (user.getAccountStatus() == EAccountStatus.LOCKED) {
            throw new APIException("User locked");
        }
        return user;
    }

    @Override
    public void assignShift(AssignShiftRequestDTO assignShiftRequestDTO) {

        User staff = getAccountUserAndStatusUser(assignShiftRequestDTO.getStaffId());

        if (workShiftRepo.existsByStaff_UserIdAndWorkDateAndShiftType(
                assignShiftRequestDTO.getStaffId(),
                assignShiftRequestDTO.getWorkDate(),
                assignShiftRequestDTO.getShiftType())) {
            throw new APIException("Nhân viên đã có ca này trong ngày");
        }

        // Giới hạn thời gian
        LocalDate today = LocalDate.now();
        LocalDate maxAllowed = today.plusMonths(3);

        LocalDate workDate = assignShiftRequestDTO.getWorkDate();

        if (workDate.isBefore(today)) {
            throw new APIException("Không thể phân ca cho ngày trong quá khứ");
        }

        if (workDate.isAfter(maxAllowed)) {
            throw new APIException("Chỉ được phân ca trong vòng 3 tháng kể từ hôm nay");
        }

        // Tạo ca làm
        WorkShift shift = new WorkShift();
        shift.setStaff(staff);
        shift.setWorkDate(workDate);
        shift.setShiftType(assignShiftRequestDTO.getShiftType());
        shift.setNote(assignShiftRequestDTO.getNote());

        if (assignShiftRequestDTO.getFloors() != null) {
            shift.getFloors().addAll(assignShiftRequestDTO.getFloors());
        }

        WorkShift savedShift = workShiftRepo.save(shift);
        webSocket("SHIFT_ASSIGNED", modelMapper.map(savedShift, WorkShiftDTO.class));
    }

    @Override
    public void assignWeeklyShifts(
            Long staffId,
            LocalDate weekStart,
            Map<DayOfWeek, ShiftType> shifts
    ) {
        User staff = getAccountUserAndStatusUser(staffId);
        for (Map.Entry<DayOfWeek, ShiftType> entry : shifts.entrySet()) {

            LocalDate workDate = weekStart.with(entry.getKey());
            ShiftType shiftType = entry.getValue();

            if (workShiftRepo.existsByStaff_UserIdAndWorkDateAndShiftType(
                    staffId, workDate, shiftType)) {
                continue; // bỏ qua ca trùng
            }

            WorkShift shift = new WorkShift();
            shift.setStaff(staff);
            shift.setWorkDate(workDate);
            shift.setShiftType(shiftType);

            WorkShift workShift = workShiftRepo.save(shift);
            webSocket("SHIFT_ASSIGNED_WEEK", modelMapper.map(workShift, WorkShiftDTO.class));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkShiftDTO> getMySchedule(
            LocalDate from,
            LocalDate to
    ) {
        Long userId = SecuritySnapshotUtil.getUserId();
        User user = getAccountUserAndStatusUser(userId);
        return workShiftRepo.findMyScheduleEntity(
                        userId, from, to
                ).stream()
                .map(ws -> {
                    WorkShiftDTO workShiftDTO = new WorkShiftDTO();
                    workShiftDTO.setWorkShiftId(ws.getWorkShiftId());
                    workShiftDTO.setShiftType(ws.getShiftType());
                    workShiftDTO.setWorkDate(ws.getWorkDate());
                    workShiftDTO.setStaffName(ws.getStaff().getUserName());
                    workShiftDTO.setFloors(ws.getFloors());
                    workShiftDTO.setNote(ws.getNote());
                    return workShiftDTO;
                }).toList();
    }

    @Override
    public void updateShift(
            Long workShiftId,
            ShiftType newShiftType
    ) {
        WorkShift shift = workShiftRepo.findById(workShiftId)
                .orElseThrow(() -> new APIException("Không tìm thấy ca"));

        //Check trùng khi đổi ca
        if (workShiftRepo.existsByStaff_UserIdAndWorkDateAndShiftType(
                shift.getStaff().getUserId(),
                shift.getWorkDate(),
                newShiftType)) {
            throw new APIException("Ca mới bị trùng lịch");
        }
        shift.setShiftType(newShiftType);
        WorkShift savedShift = workShiftRepo.save(shift);
        WorkShiftDTO dto = modelMapper.map(
                savedShift,
                WorkShiftDTO.class
        );

        webSocket("SHIFT_UPDATED", dto);
    }

    @Override
    public void deactivateShift(Long workShiftId) {
        WorkShift shift = workShiftRepo.findById(workShiftId)
                .orElseThrow(() -> new APIException("Không tìm thấy ca"));

        shift.setActive(false);

        WorkShift savedShift = workShiftRepo.save(shift);

        // ===== WEBSOCKET =====
        messagingTemplate.convertAndSend(
                "/topic/work-shift",
                Map.of(
                        "event", "SHIFT_CANCELLED",
                        "data", Map.of(
                                "workShiftId", savedShift.getWorkShiftId(),
                                "staffId", savedShift.getStaff().getUserId(),
                                "workDate", savedShift.getWorkDate(),
                                "shiftType", savedShift.getShiftType(),
                                "active", false
                        )
                )
        );
    }

    @Transactional
    public void assignBulkShift(BulkAssignShiftRequestDTO request) {

        String currentRole = SecuritySnapshotUtil.getRole();
        if (!"ADMIN".equals(currentRole)) {
            throw new APIException("Chỉ ADMIN mới được phân lịch");
        }

        for (Long staffId : request.getStaffIds()) {

            User staff = getAccountUserAndStatusUser(staffId);
            boolean exists = workShiftRepo
                    .existsByStaff_UserIdAndWorkDateAndShiftType(
                            staffId,
                            request.getWorkDate(),
                            request.getShiftType()
                    );

            if (exists) {
                continue; // bỏ qua nhân viên đã có ca
            }

            WorkShift shift = new WorkShift();
            shift.setStaff(staff);
            shift.setWorkDate(request.getWorkDate());
            shift.setShiftType(request.getShiftType());
            shift.setNote(request.getNote());

            if (request.getFloors() != null) {
                shift.getFloors().addAll(request.getFloors());
            }

            WorkShift saved = workShiftRepo.save(shift);

            webSocket("SHIFT_ASSIGNED_BULK", modelMapper.map(saved, WorkShiftDTO.class));
        }
    }

    @Transactional
    public ShiftGroup createShiftGroup(CreateShiftGroupRequest request) {

        if (!"ADMIN".equals(SecuritySnapshotUtil.getRole())) {
            throw new APIException("Chỉ ADMIN mới được phân lịch");
        }

        ShiftGroup group = new ShiftGroup();
        group.setWorkDate(request.getWorkDate());
        group.setShiftType(request.getShiftType());
        group.setFloors(request.getFloors());
        group.setNote(request.getNote());
        group.setCreatedBy(SecuritySnapshotUtil.getUserId());
        group.setCreatedAt(LocalDateTime.now());
        group.setActive(true);

        ShiftGroup savedGroup = shiftGroupRepo.save(group);

        for (Long staffId : request.getStaffIds()) {

            boolean exists = workShiftRepo
                    .existsByStaff_UserIdAndWorkDateAndShiftType(
                            staffId,
                            request.getWorkDate(),
                            request.getShiftType()
                    );

            if (exists) continue;

            User staff = getAccountUserAndStatusUser(staffId);

            WorkShift shift = new WorkShift();
            shift.setStaff(staff);
            shift.setWorkDate(request.getWorkDate());
            shift.setShiftType(request.getShiftType());
            shift.setFloors(request.getFloors());
            shift.setNote(request.getNote());
            shift.setShiftGroup(savedGroup);

            workShiftRepo.save(shift);
        }
        webSocket("SHIFT_ASSIGNED_BULK", savedGroup);
        return savedGroup;
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeScheduleResponse getEmployeesByScheduleStatus(
            LocalDate from,
            LocalDate to,
            boolean hasSchedule,
            Integer pageNumber,
            Integer pageSize,
            String sortBy,
            String sortOrder,
            String userName
    ) {

        if (pageNumber < 0 || pageSize <= 0) {
            throw new APIException("Thông tin phân trang không hợp lệ");
        }

        Sort sortByAndOrderBy = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sortByAndOrderBy);


        String keyword = (userName == null || userName.trim().isEmpty())
                ? null
                : userName.trim();


        Page<UserShiftDTO> pageUsers;


        if (hasSchedule) {
            pageUsers = userRepo.findUserShiftsFlat(
                    from, to, keyword, pageDetails
            );
            pageUsers.getContent().forEach(dto -> {
                WorkShift ws = workShiftRepo.findShiftWithFloors(
                        dto.getUserId(),
                        dto.getWorkDate(),
                        dto.getShiftType()
                );

                if (ws != null) {
                    dto.setFloors(ws.getFloors());
                }
            });

        } else {
            pageUsers = userRepo.findEmployeesWithoutScheduleDTO(
                    from, to, keyword, pageDetails
            );
        }

        if (pageUsers.isEmpty()) {
            throw new APIException("Không tìm thấy nhân viên phù hợp");
        }

        // ===== RESPONSE =====
        EmployeeScheduleResponse response = new EmployeeScheduleResponse();
        response.setContent(pageUsers.getContent());
        response.setPageNumber(pageUsers.getNumber());
        response.setPageSize(pageUsers.getSize());
        response.setTotalElement(pageUsers.getTotalElements());
        response.setTotalPage(pageUsers.getTotalPages());

        return response;
    }

    //thống kê ca làm theo buổi
    @Transactional(readOnly = true)
    public ShiftStatisticResponse getShiftStatistic(LocalDate from, LocalDate to) {
        Long userId = SecuritySnapshotUtil.getUserId();
        if (userId == null) {
            throw new APIException("Not found user");
        }
        User user = getAccountUserAndStatusUser(userId);
        List<WorkShiftOverview> statistics = workShiftRepo.getShiftStatistic(from, to);
        ShiftStatisticResponse statisticResponse = new ShiftStatisticResponse();
        statisticResponse.setTo(to);
        statisticResponse.setFrom(from);
        statisticResponse.setContent(statistics);
        return statisticResponse;
    }

    //not yet create api
    public String deleteWorkShift(Long workShiftId,Boolean force){
        WorkShift workShift=workShiftRepo.findById(workShiftId).orElseThrow(()->new APIException("The work schedule"));
        if(!force && workShift.getStaff() != null){
            throw new APIException("WorkShift already has employees");
        }
        userRepo.deleteById(workShiftId);
        return "Delete success";
    }
    public Map<Long,Long> findStopStaffShiftCount(
            LocalDate dataFrom, LocalDate dataTo, List<LocalDate> removalTime,Long topWorkShift){
        if(dataFrom.isAfter(dataTo)){
            throw new APIException("The time format is incorrect; it should be "+ dataFrom +" instead of "+ dataTo);
        }
        List<WorkShift> workShift=workShiftRepo.findByWorkDateBetween(dataFrom,dataTo);
        if(removalTime!=null){
            for(LocalDate d:removalTime){
                if(d.isBefore(dataFrom)||d.isAfter(dataTo)){
                    throw new APIException("Removal data must be within range");
                }
            }
        }
        Map<Long, Long>userAndShift=new HashMap<>();
       for(WorkShift ws: workShift){
           if(removalTime!=null&&removalTime.contains(ws.getWorkDate())){
               continue;
           }
           Long userId=ws.getStaff().getUserId();
           userAndShift.put(userId,userAndShift.getOrDefault(userId,0L)+1);

       }
        Map<Long, Long >userAndShift=new HashMap<>();

    }
}
