package com.example.shop.controller;

import com.example.shop.config.AppConstants;
import com.example.shop.entity.WorkShift;

import com.example.shop.entity.enums.ShiftType;
import com.example.shop.payloads.AssignShiftRequestDTO;
import com.example.shop.payloads.AutoWorkShiftRequest;
import com.example.shop.payloads.BulkAssignShiftRequestDTO;
import com.example.shop.payloads.Event.CreateShiftGroupRequest;
import com.example.shop.payloads.WorkShiftDTO;
import com.example.shop.payloads.reponse.EmployeeScheduleResponse;

import com.example.shop.payloads.reponse.ShiftStatisticResponse;
import com.example.shop.payloads.reponse.WorkShiftPreviewResponse;
import com.example.shop.service.AutoWorkShiftService;
import com.example.shop.service.WorkShiftService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RequestMapping("/api")
@RestController
@SecurityRequirement(name = "E-commerce Application")
public class WorkShiftController {
    @Autowired
    WorkShiftService workShiftService;
    @Autowired AutoWorkShiftService autoWorkShiftService;
    WorkShiftDTO w=new WorkShiftDTO();
    @PostMapping("admin/assign")
    public ResponseEntity<String> assignShift(
            @Valid
            @RequestBody AssignShiftRequestDTO assignShiftRequestDTO
            ) {
        workShiftService.assignShift(assignShiftRequestDTO);
        return ResponseEntity.ok("Phân ca thành công");
    }

    @PostMapping("admin/assign-week")
    public ResponseEntity<?> assignWeeklyShifts(
            @RequestParam Long staffId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate weekStart,
            @RequestBody Map<DayOfWeek, ShiftType> shifts
    ) {
        workShiftService.assignWeeklyShifts(staffId, weekStart, shifts);
        return ResponseEntity.ok("Phân lịch tuần thành công");
    }

    //nhaan vieen xem lich
    @GetMapping("/employee/staff/my-schedule")
    public ResponseEntity<List<WorkShiftDTO>> getMySchedule(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return ResponseEntity.ok(
                workShiftService.getMySchedule(from, to)
        );
    }
//đổi ca
    @PutMapping("admin/{workShiftId}/update")
    public ResponseEntity<String> updateShift(
            @PathVariable Long workShiftId,
            @RequestParam ShiftType newShiftType
    ) {
        workShiftService.updateShift(workShiftId, newShiftType);
        return ResponseEntity.ok("Cập nhật ca thành công");
    }
    //huy ca
    @PutMapping("admin/{workShiftId}/deactivate")
    public ResponseEntity<String> deactivateShift(
            @PathVariable Long workShiftId
    ) {
        workShiftService.deactivateShift(workShiftId);
        return ResponseEntity.ok("Đã hủy ca làm");
    }

    @PostMapping("/admin/work-shifts/assign-bulk")
    public ResponseEntity<?> assignBulkShift(
            @RequestBody BulkAssignShiftRequestDTO request
    ) {
        workShiftService.assignBulkShift(request);
        return ResponseEntity.ok("Phân công thành công");
    }

    @PostMapping("/admin/work-shifts/assign-bulk/group")
    public ResponseEntity<String> assignShiftBulk(
            @RequestBody CreateShiftGroupRequest request
    ) {
        workShiftService.createShiftGroup(request);
        return ResponseEntity.ok("Phân lịch thành công");
    }

    @GetMapping("admin/schedule-status")
    public ResponseEntity<EmployeeScheduleResponse> filterEmployeesBySchedule(
            @RequestParam (required = false) LocalDate from,
            @RequestParam (required = false) LocalDate to,
            @RequestParam boolean hasSchedule,
            @RequestParam(defaultValue = "0") Integer pageNumber,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = AppConstants.SORT_WORK_SCHEDULE) String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(required = false) String userName
    ) {
        return ResponseEntity.ok(
                workShiftService.getEmployeesByScheduleStatus(
                        from,
                        to,
                        hasSchedule,
                        pageNumber,
                        pageSize,
                        sortBy,
                        sortOrder,
                        userName
                )
        );
    }


    @GetMapping("admin/work-shifts/overview")
    public ResponseEntity<ShiftStatisticResponse> getOverview(@RequestParam LocalDate to,@RequestParam LocalDate from){
        return ResponseEntity.ok(workShiftService.getShiftStatistic(from,to));
    }

    @PostMapping("/admin/auto-workshift/apply")
    public ResponseEntity<?> apply(
            @RequestBody AutoWorkShiftRequest request
    ) {

        autoWorkShiftService.autoSchedule(
                request.getFrom(),
                request.getTo(),
                request.getUserId(),
                request.getDates()
        );

        return ResponseEntity.ok("Schedule applied");
    }

    @PostMapping("admin/auto-workshift/preview")
    public ResponseEntity<List<WorkShiftPreviewResponse>> preview(
            @Valid @RequestBody AutoWorkShiftRequest request) {

        return ResponseEntity.ok(
                autoWorkShiftService.previewSchedule(
                        request.getFrom(),
                        request.getTo(),
                        request.getUserId(),
                        request.getDates()
                )
        );
    }


}
