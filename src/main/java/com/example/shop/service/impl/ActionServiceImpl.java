package com.example.shop.service.impl;

import com.example.shop.entity.ActivityLog;
import com.example.shop.entity.User;
import com.example.shop.entity.enums.EActivityResult;
import com.example.shop.exception.APIException;
import com.example.shop.hellper.SecuritySnapshotUtil;
import com.example.shop.payloads.ActivityLogResponseDTO;
import com.example.shop.payloads.reponse.ActiveLogResponse;
import com.example.shop.repository.ActivityRepo;
import com.example.shop.repository.UserRepo;
import com.example.shop.service.ActivityLogService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ActionServiceImpl implements ActivityLogService {
    @Autowired
    ActivityRepo activityRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    UserRepo userRepo;

    @Override
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
    ) {
        Long userId = SecuritySnapshotUtil.getUserId();
        User user = null;
        if (userId != null && userRepo.existsById(userId)) {
            user = userRepo.getReferenceById(userId);
        }

        ActivityLog log = ActivityLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldData(oldData)
                .newData(newData)
                .performedAt(LocalDateTime.now())
                .performedBy(
                        SecuritySnapshotUtil.isAuthenticated()
                                ? SecuritySnapshotUtil.getEmployeeName()
                                : "SYSTEM"
                )
                .performerRole(
                        SecuritySnapshotUtil.isAuthenticated()
                                ? SecuritySnapshotUtil.getRole()
                                : "SYSTEM"
                )
                .employeeName(employeeName)
                .customerPhone(customerPhone)
                .result(result)
                .note(note)
                .user(user)
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

    @Override
    public Page<ActivityLogResponseDTO> getMyActivities(
            String entityType,
            String keyword,
            int page,
            int size
    ) {
        Long userId = SecuritySnapshotUtil.getUserId();

        Page<ActivityLog> logs = activityRepo.findMyActivities(
                userId,
                entityType,
                keyword,
                PageRequest.of(page, size)
        );


        return logs.map(activityLog ->
                modelMapper.map(activityLog, ActivityLogResponseDTO.class)
        );
    }

    @Override
    public byte[] exportMyActivitiesToExcel(
            String entityType,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        Long userId = SecuritySnapshotUtil.getUserId();

        List<ActivityLog> logs = activityRepo.findMyActivitiesForExport(
                userId,
                entityType,
                fromDate,
                toDate
        );

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("My Activity Logs");

            // ================== STYLES ==================
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            Font normalFont = workbook.createFont();
            normalFont.setFontHeightInPoints((short) 11);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle normalStyle = workbook.createCellStyle();
            normalStyle.setFont(normalFont);
            normalStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.cloneStyleFrom(normalStyle);
            wrapStyle.setWrapText(true);

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.cloneStyleFrom(normalStyle);
            dateStyle.setDataFormat(
                    workbook.getCreationHelper()
                            .createDataFormat()
                            .getFormat("yyyy-MM-dd HH:mm:ss")
            );

            CellStyle successStyle = workbook.createCellStyle();
            successStyle.cloneStyleFrom(normalStyle);
            successStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            successStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle failStyle = workbook.createCellStyle();
            failStyle.cloneStyleFrom(normalStyle);
            failStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            failStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ================== HEADER ==================
            String[] columns = {
                    "Thời gian",
                    "Loại",
                    "Hành động",
                    "Đối tượng ID",
                    "Người thực hiện",
                    "Vai trò",
                    "Dữ liệu thao tác trước",
                    "Dữ liệu sau thao tác",
                    "Kết quả",
                    "Ghi chú"
            };

            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // ================== DATA ==================
            int rowIdx = 1;
            for (ActivityLog log : logs) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(40);

                Cell timeCell = row.createCell(0);
                timeCell.setCellValue(log.getPerformedAt());
                timeCell.setCellStyle(dateStyle);

                row.createCell(1).setCellValue(log.getEntityType());
                row.createCell(2).setCellValue(log.getAction());
                row.createCell(3).setCellValue(
                        log.getEntityId() != null ? log.getEntityId() : 0
                );
                row.createCell(4).setCellValue(log.getPerformedBy());
                row.createCell(5).setCellValue(log.getPerformerRole());

                boolean supportBeforeAfter = Set.of(
                        "PAY_CASH", "UPDATE_CART", "CASH_PAYMENT", "CHECK_OUT"
                ).contains(log.getAction());

                Cell oldCell = row.createCell(6);
                oldCell.setCellValue(
                        supportBeforeAfter && StringUtils.hasText(log.getOldData())
                                ? log.getOldData()
                                : ""
                );
                oldCell.setCellStyle(wrapStyle);

                Cell newCell = row.createCell(7);
                newCell.setCellValue(
                        supportBeforeAfter && StringUtils.hasText(log.getNewData())
                                ? log.getNewData()
                                : ""
                );
                newCell.setCellStyle(wrapStyle);

                Cell resultCell = row.createCell(8);
                resultCell.setCellValue(log.getResult().name());
                resultCell.setCellStyle(
                        log.getResult() == EActivityResult.SUCCESS
                                ? successStyle
                                : failStyle
                );

                Cell noteCell = row.createCell(9);
                noteCell.setCellValue(log.getNote());
                noteCell.setCellStyle(wrapStyle);
            }

            // ================== SHEET SETUP ==================
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(
                    new CellRangeAddress(0, 0, 0, columns.length - 1)
            );

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Không thể export Excel", e);
        }
    }



}
