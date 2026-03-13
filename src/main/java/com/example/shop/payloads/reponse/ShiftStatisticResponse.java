package com.example.shop.payloads.reponse;

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
public class ShiftStatisticResponse {
    LocalDate to;
    LocalDate from;
    List<WorkShiftOverview> content;
}
