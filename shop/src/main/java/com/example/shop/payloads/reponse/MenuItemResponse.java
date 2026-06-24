package com.example.shop.payloads.reponse;

import com.example.shop.payloads.MenuItemDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@NoArgsConstructor
@Data
public class MenuItemResponse {
    private List<MenuItemDTO>content;
    private Integer pageNumber;
    private Integer pageSize;
    private Long totalElement;
    private Integer totalPage;
    private boolean lastPage;
}
