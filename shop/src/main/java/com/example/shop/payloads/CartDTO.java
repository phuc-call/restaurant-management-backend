package com.example.shop.payloads;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CartDTO {
    private Long id;
    private Long tableId;
    private String restaurantTable;
    private String status;
    List<CartItemDTO>cartItems;
    private BigDecimal totalPrice;
}
