package com.example.shop.service;

import com.example.shop.entity.WorkShift;
import com.example.shop.payloads.AutoWorkShiftRequest;
import com.example.shop.payloads.reponse.WorkShiftPreviewResponse;

import java.time.LocalDate;
import java.util.List;

public interface AutoWorkShiftService {
    public void autoSchedule(LocalDate from, LocalDate to, List<Long> userStaff, List<LocalDate> dates);
    public List<WorkShiftPreviewResponse>previewSchedule(LocalDate from, LocalDate to, List<Long>userStaffId, List<LocalDate>dates);
}
