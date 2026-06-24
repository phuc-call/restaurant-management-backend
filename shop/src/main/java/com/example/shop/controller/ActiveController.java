package com.example.shop.controller;

import com.example.shop.config.AppConstants;
import com.example.shop.payloads.reponse.ActiveLogResponse;
import com.example.shop.service.ActivityLogService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

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

}
