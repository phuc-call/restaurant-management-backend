package com.example.shop.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ServiceOrderDTO {
    private Long orderId;
    private String tableName;
    private String noteOrder;
    private String billCode;
    private List<ServiceOrderItemDTO> items;
}
