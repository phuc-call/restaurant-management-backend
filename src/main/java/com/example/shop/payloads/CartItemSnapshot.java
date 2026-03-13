package com.example.shop.payloads;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CartItemSnapshot {
    private Long menuItemId;
    private String menuItemName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private String note;
    private BigDecimal discount;
    private BigDecimal totalPrice;
}