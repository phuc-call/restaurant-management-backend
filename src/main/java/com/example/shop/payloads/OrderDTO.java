package com.example.shop.payloads;


import com.example.shop.entity.enums.PaymentMethod;
import com.example.shop.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {
    private Long id;

    private Long cartId;
    private Long tableId;
    private String tableName;
    private Long userId;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private String customerName;
    private BigDecimal totalPrice;
    private String status;
    private String cashierName;
    private LocalDateTime paidAt;
    private List<OrderItemDTO> items;
}
