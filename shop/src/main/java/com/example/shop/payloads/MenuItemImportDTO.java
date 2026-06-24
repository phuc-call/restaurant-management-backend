package com.example.shop.payloads;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MenuItemImportDTO {
    private String name;
    private BigDecimal price;
    private String description;
    private Long categoryId;
    private List<String>imageNames;
}
