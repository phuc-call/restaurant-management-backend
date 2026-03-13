package com.example.shop.payloads;

import com.example.shop.entity.enums.Floor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CreateMultipleTableDTO {
    private int quantity;
    private Long startNumber;
    private String tableNamePrefix;
    private Long seatCount;   // 👈 ĐỔI Integer → Long
    private Long tableTypeId;

}

