package com.example.shop.controller;

import com.example.shop.config.AppConstants;
import com.example.shop.entity.RestaurantTable;
import com.example.shop.entity.enums.TableStatus;
import com.example.shop.exception.APIException;
import com.example.shop.payloads.RestaurantTableDTO;
import com.example.shop.payloads.reponse.MenuItemResponse;
import com.example.shop.payloads.reponse.RestaurantTableResponse;
import com.example.shop.repository.RestaurantRepo;
import com.example.shop.repository.TableTypeRepo;
import com.example.shop.service.MenuItemService;
import com.example.shop.service.RestaurantTableService;
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
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-commerce Application")
public class TableController {
    @Autowired
    RestaurantTableService restaurantTableService;
    @Autowired
    RestaurantRepo restaurantRepo;

    // Lấy danh sách tất cả bàn
    @GetMapping("/employee/staff/tables")
    public ResponseEntity<RestaurantTableResponse> getAllTables(
            @Valid
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_TABLE_ID) String sortBy,
            @RequestParam(name = "order", defaultValue = AppConstants.SORT_DIR) String order,
            @RequestParam(name = "status", required = false) TableStatus status
    ) {

        RestaurantTableResponse response =
                restaurantTableService.getAllRestaurantTable(pageNumber, pageSize, sortBy, order,status);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/employee/staff/{tableId}/table")
    public ResponseEntity<String> deletedTable(@PathVariable Long tableId) {
        String deletedTable = restaurantTableService.delete(tableId);
        return ResponseEntity.ok(deletedTable);
    }

    @PostMapping(value = "/admin/tables",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RestaurantTableDTO> createTable(
            @Valid
            @RequestPart("dto") String json,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws JsonProcessingException {
        ObjectMapper mapper=new ObjectMapper();
        RestaurantTableDTO dto=mapper.readValue(json,RestaurantTableDTO.class);
        RestaurantTableDTO result=restaurantTableService.createTable(dto,files);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    @PutMapping("/employee/manager/tables/{tableId}/status")
    public ResponseEntity<String>changeStatus(@PathVariable Long tableId){
        String restaurantTable=restaurantTableService.setStatusTableAVAILABLE(tableId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/public/tables/{tableId}/qr")
    public ResponseEntity<byte[]> getQr(@PathVariable Long tableId) {
        RestaurantTable table = restaurantRepo.findById(tableId)
                .orElseThrow(() -> new APIException("Table not found"));

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(table.getQrImage());
    }

}
