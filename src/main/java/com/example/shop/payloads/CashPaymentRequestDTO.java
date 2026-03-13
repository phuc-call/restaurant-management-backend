package com.example.shop.payloads;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
public class CashPaymentRequestDTO {
    private Long orderId;
    private BigDecimal cashReceived; // CHỈ DÙNG TÍNH TOÁN
}
