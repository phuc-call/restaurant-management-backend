package com.example.shop.service;

import com.example.shop.entity.ShiftGroup;
import com.example.shop.entity.User;
import com.example.shop.entity.WorkShift;
import com.example.shop.entity.enums.Floor;
import com.example.shop.entity.enums.ShiftType;
import com.example.shop.payloads.AssignShiftRequestDTO;
import com.example.shop.payloads.BulkAssignShiftRequestDTO;
import com.example.shop.payloads.Event.CreateShiftGroupRequest;
import com.example.shop.payloads.WorkShiftDTO;
import com.example.shop.payloads.reponse.EmployeeScheduleResponse;
import com.example.shop.payloads.reponse.ShiftStatisticResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface WorkShiftService {
    // Phân ca cho 1 ngày
    public void assignShift(
            AssignShiftRequestDTO assignShiftRequestDTO
    );

    // Phân lịch theo tuần
    void assignWeeklyShifts(
            Long staffId,
            LocalDate weekStart,
            Map<DayOfWeek, ShiftType> shifts
    );

    // Xem lịch của 1 nhân viên
    public List<WorkShiftDTO> getMySchedule(
            LocalDate from,
            LocalDate to
    );

    // Sửa ca
    void updateShift(
            Long workShiftId,
            ShiftType newShiftType
    );

    // Hủy ca
    void deactivateShift(Long workShiftId);
    void assignBulkShift(BulkAssignShiftRequestDTO request);
    ShiftGroup createShiftGroup(CreateShiftGroupRequest request);

    EmployeeScheduleResponse getEmployeesByScheduleStatus(
            LocalDate from,
            LocalDate to,
            boolean hasSchedule,
            Integer pageNumber,
            Integer pageSize,
            String sortBy,
            String sortOrder,
            String userName
    );
    ShiftStatisticResponse getShiftStatistic(LocalDate to,LocalDate from);

}
