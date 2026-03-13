package com.example.shop.payloads;

import com.example.shop.entity.enums.Floor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CartDTO {
    private Long cartId;
    private Long tableId;
    private String restaurantTable;
    private String status;
    private Floor floops;
    private BigDecimal totalPrice;
    private LocalDateTime updatedAt;
    private List<CartItemDTO> items;
    private String Customer;
}
