package com.example.shop.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableTypeDTO {
    private String name;
    private String description;
    private Long seatCount;
    private Double extraFee;
}
