package com.example.shop.payloads;

import com.example.shop.entity.enums.EOrderItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KitchenOrderFlatDTO {

    // Order
    private Long orderId;
    private String billCode;
    private String tableName;
    private String noteOrder;
    private String kitchenStaffName;

    // OrderItem
    private Long orderItemId;
    private String menuName;
    private Integer quantity;
    private String nameTable;
    private String itemNote;
    private EOrderItem status;
}