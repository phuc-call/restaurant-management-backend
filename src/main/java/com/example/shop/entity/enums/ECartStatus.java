package com.example.shop.entity.enums;

public enum ECartStatus {
    ACTIVE,        // cart trống, sẵn sàng dùng
    ORDERING,      // khách đang gọi món
    READY_TO_PAY,  // khách yêu cầu thanh toán
    IN_PROGRESS,
    CLOSED         // đã thu tiền, kết thúc
}


