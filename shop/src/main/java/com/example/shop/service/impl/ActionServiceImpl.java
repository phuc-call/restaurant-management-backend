package com.example.shop.service.impl;

import com.example.shop.entity.ActivityLog;
import com.example.shop.exception.APIException;
import com.example.shop.hellper.SecuritySnapshotUtil;
import com.example.shop.payloads.reponse.ActiveLogResponse;
import com.example.shop.repository.ActivityRepo;
import com.example.shop.service.ActivityLogService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class ActionServiceImpl implements ActivityLogService {
    @Autowired
    ActivityRepo activityRepo;
    @Autowired
    ModelMapper modelMapper;

    @Override
    public void log(
            String entityType,
            Long entityId,
            String action,
            String oldData,
            String newData,
            String customerName,
            String customerPhone,
            String note
    ) {
        ActivityLog log = ActivityLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldData(oldData)
                .newData(newData)
                .performedAt(LocalDateTime.now())
                .performedBy(
                        SecuritySnapshotUtil.isAuthenticated()
                                ? SecuritySnapshotUtil.getUserName()
                                : "SYSTEM"
                )
                .performerRole(
                        SecuritySnapshotUtil.isAuthenticated()
                                ? SecuritySnapshotUtil.getRole()
                                : "SYSTEM"
                )
                .customerName(customerName)
                .customerPhone(customerPhone)
                .note(note)
                .build();

        activityRepo.save(log);
    }

    @Override
    public Page<ActiveLogResponse> getLogs(
            Integer pageNumber,
            Integer pageSize,
            String sortBy,
            String sortOrder,
            String entityType,
            String action,
            String performerRole,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        Sort sort = "asc".equalsIgnoreCase(sortOrder)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageDetails = PageRequest.of(
                pageNumber != null ? pageNumber : 0,
                pageSize != null ? pageSize : 10,
                sort
        );
        Page<ActivityLog> logs = activityRepo.findLogs(
                entityType,
                action,
                performerRole,
                fromDate,
                toDate,
                pageDetails
        );
        return logs.map(log -> modelMapper.map(log, ActiveLogResponse.class));
    }

    public ActiveLogResponse getLogDetail(Long logId) {
        ActivityLog activityLog = activityRepo.findById(logId).orElseThrow(() -> {
            return new APIException("Không tìm thấy lịch sử này");
        });
        String role = SecuritySnapshotUtil.getRole();

        return modelMapper.map(activityLog, ActiveLogResponse.class);
    }

    @Override
    public void deleteLog(Long logId) {
        ActivityLog activityLog = activityRepo.findById(logId).orElseThrow(() -> {
            return new APIException("Không tìm thấy lịch sử này");
        });
        String role = SecuritySnapshotUtil.getRole();
        activityRepo.deleteById(logId);
    }

    @Override
    public int deleteLogsBefore(LocalDateTime beforeDate) {
        if (beforeDate == null) {
            throw new APIException("Thời điểm xóa log không hợp lệ");
        }
        String role = SecuritySnapshotUtil.getRole();
        return activityRepo.deleteLogsBefore(beforeDate);
    }

    @Override
    public int deleteLogsByEntity(String entityType, Long entityId) {

        if (entityType == null || entityType.isBlank() || entityId == null) {
            throw new APIException("Thông tin entity không hợp lệ");
        }

        // Chỉ ADMIN mới được phép xóa log theo entity
        String role = SecuritySnapshotUtil.getRole();

        return activityRepo.deleteByEntity(entityType, entityId);
    }


}
