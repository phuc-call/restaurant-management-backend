package com.example.shop.payloads.Event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@AllArgsConstructor
public class PaymentEventDTO {
    private String event;        // PAID
    private Long tableId;
    private Long orderId;
    private BigDecimal amount;
    private String sound;        // momo / pig / success
}
