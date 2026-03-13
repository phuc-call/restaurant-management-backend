package com.example.shop.controller;

import com.example.shop.config.AppConstants;
import com.example.shop.payloads.CategoryDTO;
import com.example.shop.payloads.reponse.CategoryResponse;
import com.example.shop.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-commerce Application")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @GetMapping("/public/categories")
    public ResponseEntity<CategoryResponse> getAllCategory(
            @Valid
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CATEGORY_BY, required = false) String sortBy,
            @RequestParam(name = "order", defaultValue = AppConstants.SORT_DIR, required = false) String order) {
        CategoryResponse categoryResponse = categoryService.getAllCategory(pageNumber, pageSize, sortBy, order);
        return new ResponseEntity<>(categoryResponse, HttpStatus.OK);
    }

    @PostMapping(
            value = "/admin/category",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE )
    public ResponseEntity<CategoryDTO> createCategory(
            @Valid
            @RequestPart("dto") String json,
            @RequestPart(value = "files", required = false) List<MultipartFile> files )
            throws Exception {
///       Multipart không phải JSON thuần, nên Spring KHÔNG thể tự convert
///        JSON trong một "part" sang DTO như @RequestBody.
//        Manual JSON Deserialization(tự giải mã JSON từ chuỗi String)
        ObjectMapper mapper = new ObjectMapper();
        CategoryDTO dto = mapper.readValue(json, CategoryDTO.class);
        CategoryDTO result = categoryService.createCategory(dto, files);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    public ResponseEntity<String>deleteCategory(
            @Valid
            @PathVariable Long categoryId){
        String DeleteCategory=categoryService.deleteCategory(categoryId);
        return new ResponseEntity<>(DeleteCategory,HttpStatus.NO_CONTENT);
    }
    @GetMapping("public/categories/{categoryId}")
    public ResponseEntity<CategoryDTO>getCategoryById(@Valid @PathVariable Long categoryId){
        CategoryDTO categoryDTO=categoryService.getCategoryById(categoryId);
        return new ResponseEntity<>(categoryDTO,HttpStatus.OK);
    }
}
