package com.example.shop.service;

import com.example.shop.entity.MenuItem;
import com.example.shop.payloads.MenuItemDTO;
import com.example.shop.payloads.MenuItemRequestDTO;
import com.example.shop.payloads.reponse.MenuItemResponse;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface MenuItemService {
    MenuItemResponse getAllMenu(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder,String keyword,
                                Long categoryId, BigDecimal minPrice, BigDecimal maxPrice);

    MenuItemDTO createMenuItem(MenuItemRequestDTO dto, List<MultipartFile> files);

    MenuItemDTO getOneMenu(Long id);

    MenuItemDTO updateMenu(Long id, MenuItemDTO menuItemDTO,List<MultipartFile>files);

    String deleteMenu(Long id);

//    List<MenuItemResponse> getByCategory(Integer pageNumber,Integer pageSize, String sortBy,String sortOrder);

//    MenuItemDTO createMenuItem(MenuItemRequestDTO dto, List<MultipartFile> files);

}
