package com.example.shop.payloads;


import com.example.shop.entity.enums.EOrderItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KitChenOrderItemDTO {
    private Long orderItemId;
    private String menuName;
    private Integer quantity;
    private String nameTable;
    private String noteOrder;
    private EOrderItem status;
}
