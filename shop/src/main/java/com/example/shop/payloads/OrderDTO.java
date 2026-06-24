package com.example.shop.payloads;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
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
    private String customerName;
    private BigDecimal totalPrice;
    private String status;
    private List<OrderItemDTO> items;
}
