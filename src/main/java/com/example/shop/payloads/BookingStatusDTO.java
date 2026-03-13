package com.example.shop.payloads;

import com.example.shop.entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@AllArgsConstructor
@NoArgsConstructor

@Setter
@Getter
public class BookingStatusDTO {
    private BookingStatus status;
}
