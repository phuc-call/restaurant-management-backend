package com.example.shop.controller;

import com.example.shop.config.AppConstants;
import com.example.shop.payloads.ActivityLogResponseDTO;
import com.example.shop.payloads.reponse.ActiveLogResponse;
import com.example.shop.service.ActivityLogService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.print.attribute.standard.Media;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "E-commerce Application")
public class ActiveController {
    @Autowired
    ActivityLogService activityLogService;


    @GetMapping("admin/activity")
    public Page<ActiveLogResponse> getLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String performerRole,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = "performedAt") String sortBy,
            @RequestParam(name = "order", defaultValue = "desc") String order
    ) {
        return activityLogService.getLogs(
                pageNumber,
                pageSize,
                sortBy,
                order,
                entityType,
                action,
                performerRole,
                fromDate,
                toDate

        );
    }
    @GetMapping("admin/activity/{logId}")
    public ActiveLogResponse getLogDetail(@PathVariable Long logId) {
        return activityLogService.getLogDetail(logId);
    }

    @DeleteMapping("admin/activity/{logId}")
    public void deleteLog(@PathVariable Long logId) {
        activityLogService.deleteLog(logId);
    }

    @DeleteMapping("admin/activity/delete-before")
    public int deleteLogsBefore(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime beforeDate
    ) {
        return activityLogService.deleteLogsBefore(beforeDate);
    }

    @DeleteMapping("admin/activity/delete-by-entity")
    public int deleteLogsByEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId
    ) {
        return activityLogService.deleteLogsByEntity(entityType, entityId);
    }


    @GetMapping("/employee/staff/my-activity/log")
    public Page<ActivityLogResponseDTO> myActivities(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return activityLogService.getMyActivities(
                entityType,
                keyword,
                page,
                size
        );
    }
    @GetMapping("/employee/staff/my-activity/log/export")
    public ResponseEntity<byte[]> exportMyActivity(
            @RequestParam(required = false) String entityType,
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam (required = false) String nameFile
    ) {
        String fileName="my-activity-log.xlsx";
        if (nameFile != null && !nameFile.trim().isEmpty()) {
            nameFile = nameFile.trim();
            if (!nameFile.toLowerCase().endsWith(".xlsx")) {
                nameFile += ".xlsx";
            }
            fileName = nameFile;
        }

        LocalDateTime from = LocalDateTime.parse(fromDate);
        LocalDateTime to = LocalDateTime.parse(toDate);

        byte[] file = activityLogService.exportMyActivitiesToExcel(
                entityType,
                from,
                to
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""+fileName+"\"")
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(file);
    }


}
