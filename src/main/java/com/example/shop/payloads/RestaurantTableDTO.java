package com.example.shop.payloads;

import com.example.shop.entity.TableType;
import com.example.shop.entity.enums.Floor;
import com.example.shop.entity.enums.TableStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantTableDTO {
    private Long numberTable;
    private String tableName;
    private Floor floops;
    private Long seatCount;
    private String accessToken;
    private Long tableTypeId;
    private TableStatus status;
    private Long tableId;
}
