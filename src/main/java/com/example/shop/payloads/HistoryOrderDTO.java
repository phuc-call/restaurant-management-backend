package com.example.shop.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HistoryOrderDTO {
    private Long orderId;
    private String billCode;
    private String tableName;
    private String noteOrder;
    private LocalDateTime createdAt;
    private String kitchenStaffName;
    private List<KitChenOrderItemDTO> items;
}
