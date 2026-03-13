package com.example.shop.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class KitchenOrderDTO {
    private Long orderId;
    private String billCode;
    private String tableName;
    private String noteOrder;
    private String kitchenStaffName;
    private List<KitChenOrderItemDTO> items;
}
    