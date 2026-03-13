package com.example.shop.payloads.reponse;

import com.example.shop.entity.enums.ShiftType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
@Getter
@Setter
public class WorkShiftPreviewResponse {
    private Long staffId;
    private String staffName;
    private LocalDate workDate;
    private ShiftType shiftType;
}
