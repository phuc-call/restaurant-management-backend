package com.example.shop.payloads;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
public class CartItemDTO {
    private Long id;
    private Long cartId;
    private Long menuItemId;
    private String menuItemName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal subTotal; // quantity * unitPrice - discount
}
