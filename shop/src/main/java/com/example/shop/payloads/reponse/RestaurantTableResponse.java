package com.example.shop.payloads.reponse;

import com.example.shop.payloads.RestaurantTableDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantTableResponse {
    private List<RestaurantTableDTO> contents;
    private Integer pageNumber;
    private Integer pageSize;
    private Integer totalPage;
    private Long elementTotalPage;
}
