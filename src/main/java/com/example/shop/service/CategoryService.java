package com.example.shop.service;

import com.example.shop.payloads.CategoryDTO;
import com.example.shop.payloads.reponse.CategoryResponse;
import com.example.shop.payloads.reponse.MenuItemResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CategoryService {
    CategoryDTO createCategory(CategoryDTO categoryDTO,List<MultipartFile> files);
    CategoryDTO getCategoryById(Long categoryId);
    String deleteCategory(Long categoryId);
    CategoryResponse getAllCategory(Integer pageNumber,Integer pageSize,String sortBy,String sortOder);
}
