package com.example.shop.entity.enums;

public enum BookingStatus {
    PENDING,      // Khách đặt – chưa xác nhận
    CONFIRMED,    // Nhà hàng xác nhận
    CANCELLED,    // Khách / nhà hàng hủy
    NO_SHOW,      // Khách không đến
    CHECKED_IN,   // Khách đã đến, ngồi bàn
    COMPLETED     // Đã ăn xong / kết thúc
}
