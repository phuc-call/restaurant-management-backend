package com.example.shop.payloads;

import com.example.shop.entity.enums.Floor;
import com.example.shop.entity.enums.ShiftType;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotNull;
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
public class AssignShiftRequestDTO {
    @NotNull(message = "Thông tin nhân viên không được để trống")
    private Long staffId;

    @NotNull(message = "Thời gian không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDate;

    @NotNull(message = "Ca không được để trống")
    private ShiftType shiftType;

    private List<Floor> floors;

    private String note;
}
