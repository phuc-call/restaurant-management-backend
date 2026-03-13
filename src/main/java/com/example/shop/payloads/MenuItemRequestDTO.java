package com.example.shop.payloads;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MenuItemRequestDTO {
    private String name;
    private BigDecimal price;
    private String description;
    private Long categoryId;
}
