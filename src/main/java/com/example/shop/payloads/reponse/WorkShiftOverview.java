package com.example.shop.payloads.reponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class WorkShiftOverview {
   private Long userId;
   private String userName;
   private Long morningShift;
   private Long afternoonShift;
   private Long eveningShift;
   private Long totalShift;
}
