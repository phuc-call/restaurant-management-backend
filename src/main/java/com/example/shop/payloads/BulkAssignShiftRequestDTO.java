package com.example.shop.payloads;

import com.example.shop.entity.enums.Floor;
import com.example.shop.entity.enums.ShiftType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BulkAssignShiftRequestDTO {
    private List<Long> staffIds;
    private LocalDate workDate;
    private ShiftType shiftType;
    private List<Floor> floors;
    private String note;
}
