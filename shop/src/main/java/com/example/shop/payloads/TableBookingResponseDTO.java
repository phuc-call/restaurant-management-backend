package com.example.shop.payloads;

import com.example.shop.entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TableBookingResponseDTO {
    private Long bookingId;
    private String tableName;
    private Integer seatCount;
    private String customerName;
    private String customerPhone;
    private LocalDateTime bookingTime;
    private LocalDateTime endTime;
    private Integer numberOfGuests;
    private BookingStatus status;
    private String note;
    private LocalDateTime createdAt;
    private String updateBy;
    private LocalDateTime updateAt;
}
