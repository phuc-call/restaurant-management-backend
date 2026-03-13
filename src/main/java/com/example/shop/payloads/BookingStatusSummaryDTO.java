package com.example.shop.payloads;

import com.example.shop.entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
@AllArgsConstructor
@Getter
@Setter
public class BookingStatusSummaryDTO {
    private BookingStatus bookingStatus;
    private Long total;
}
