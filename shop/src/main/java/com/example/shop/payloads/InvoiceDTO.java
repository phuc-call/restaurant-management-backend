package com.example.shop.payloads;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter
public class InvoiceDTO {
    private Long orderId;
    private String tableName;
    private List<OrderItemDTO> items;

    private BigDecimal totalAmount;

    private BigDecimal cashReceived;
    private BigDecimal changeAmount;

    private String cashierName;
    private LocalDateTime paidAt;
}
