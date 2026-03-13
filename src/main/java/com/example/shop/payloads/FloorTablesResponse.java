package com.example.shop.payloads;

import com.example.shop.entity.enums.Floor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FloorTablesResponse {
    Floor floor;
    List<String> tableName;
    String message;
}
