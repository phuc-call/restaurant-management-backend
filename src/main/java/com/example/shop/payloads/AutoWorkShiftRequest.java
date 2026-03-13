package com.example.shop.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class AutoWorkShiftRequest {
    private LocalDate from;
    private LocalDate to;
    private List<Long> userId;
    private List<LocalDate> dates;
}
