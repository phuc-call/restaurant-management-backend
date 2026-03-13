package com.example.shop.service;

import com.example.shop.entity.ActivityLog;
import com.example.shop.entity.enums.EActivityResult;
import com.example.shop.payloads.ActivityLogResponseDTO;
import com.example.shop.payloads.reponse.ActiveLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ActivityLogService {

    public void log(
            String entityType,
            Long entityId,
            String action,
            String oldData,
            String newData,
            String employeeName,
            EActivityResult result,
            String customerPhone,
            String note
    );

    Page<ActiveLogResponse> getLogs(
            Integer numberPage,
            Integer pageSize,
            String sortBy,
            String sortOrder,
            String entityType,
            String action,
            String performerRole,
            LocalDateTime fromDate,
            LocalDateTime toDate
    );
    ActiveLogResponse getLogDetail(Long logId);
    void deleteLog(Long logId);
    int deleteLogsBefore(LocalDateTime beforeDate);
    int deleteLogsByEntity(String entityType, Long entityId);

    Page<ActivityLogResponseDTO> getMyActivities(
            String entityType,
            String keyword,
            int page,
            int size
    );
    byte[] exportMyActivitiesToExcel(
            String entityType,
            LocalDateTime fromDate,
            LocalDateTime toDate
    );
}