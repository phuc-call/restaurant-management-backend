package com.example.shop.controller;

import com.example.shop.config.AppConstants;
import com.example.shop.payloads.ImportResultDTO;
import com.example.shop.payloads.MenuItemDTO;
import com.example.shop.payloads.MenuItemRequestDTO;
import com.example.shop.payloads.reponse.MenuItemResponse;
import com.example.shop.service.MenuItemImportService;
import com.example.shop.service.MenuItemService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-commerce Application")
public class MenuItemController {
    @Autowired
    private MenuItemService menuItemService;
    @Autowired
    MenuItemImportService menuItemImportService;

    @PostMapping(value = "/admin/menuItem",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MenuItemDTO> createMenuItem(
            @Valid
            @RequestPart("dto") String json,
            @RequestPart(value = "file", required = false) List<MultipartFile> files)
            throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        MenuItemRequestDTO dto = mapper.readValue(json, MenuItemRequestDTO.class);
        MenuItemDTO result = menuItemService.createMenuItem(dto, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }


    @GetMapping("/public/menuItems")
    public ResponseEntity<MenuItemResponse> getMenuItems(
            @RequestParam(name = "categoryId",required = false) Long categoryId,
            @RequestParam(name = "keyword",required = false) String keyword,
            @RequestParam(name = "minPrice",required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice",required = false)BigDecimal maxPrice,
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_MENU_BY, required = false) String sortBy,
            @RequestParam(name = "order", defaultValue = AppConstants.SORT_DIR, required = false) String order) {
        MenuItemResponse menuItemResponse = menuItemService.getAllMenu(pageNumber, pageSize, sortBy, order,keyword,categoryId,minPrice,maxPrice);
        return new ResponseEntity<>(menuItemResponse, HttpStatus.OK);
    }

    @DeleteMapping("/admin/menus/{menuId}")
    public ResponseEntity<String> deleteMenuItem(
            @PathVariable Long menuId) {
        String deleteMenu = menuItemService.deleteMenu(menuId);
        return new ResponseEntity<>(deleteMenu, HttpStatus.NO_CONTENT);
    }
    @PutMapping(
            value = "/admin/menus/{menuId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<MenuItemDTO> editMenu(
            @PathVariable Long menuId,
            @RequestPart("dto") MenuItemDTO menuItemDTO,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        MenuItemDTO updated = menuItemService.updateMenu(menuId, menuItemDTO, files);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/public/menus/{menuId}")
    public ResponseEntity<MenuItemDTO> getMenuItem(@PathVariable Long menuId) {
        MenuItemDTO menuItemDTO = menuItemService.getOneMenu(menuId);
        return new ResponseEntity<>(menuItemDTO, HttpStatus.OK);
    }
//    @GetMapping("/public/menus/categories/{id}")
//    public ResponseEntity<MenuItemResponse>getMenuByCategory(
//            @RequestParam(name = "pageNumber",defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
//            @RequestParam(name = "pageSize",defaultValue = AppConstants.PAGE_SIZE,required = false) Integer pageSize,
//            @RequestParam(name = "sortBy",defaultValue = AppConstants.SORT_DIR,required = false)String sortBy,
//            @RequestParam(name = "order",defaultValue = AppConstants.SORT_CATEGORY_BY,required = false)String order
//    ){
//        MenuItemResponse menuItemResponse=menuItemService.ge
//    }
    @PostMapping(
            value = "/admin/excel/menuItem",
            consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResultDTO importMenu(
            @Valid
            @RequestPart(value = "file") MultipartFile file,
            @RequestPart(value = "images",required = false) List<MultipartFile>images
    ){
       return menuItemImportService.importMenu(file,images);
    }
}
