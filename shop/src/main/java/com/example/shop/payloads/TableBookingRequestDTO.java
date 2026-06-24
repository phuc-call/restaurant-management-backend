package com.example.shop.payloads;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TableBookingRequestDTO {
    private Long bookingId;
    @NotBlank(message = "Tên khách không được để trống")
    private String customerName;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String customerPhone;

    @NotNull(message = "Vui lòng chọn thời gian đến")
    private LocalDateTime bookingTime;

    private LocalDateTime endTime;

    @NotNull(message = "Vui lòng nhập số lượng khách")
    @Min(value = 1, message = "Số lượng khách phải lớn hơn 0")
    private Integer numberOfGuests;

    private String note;
}