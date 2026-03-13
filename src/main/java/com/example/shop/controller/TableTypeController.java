package com.example.shop.controller;

import com.example.shop.entity.Cart;
import com.example.shop.entity.RestaurantTable;
import com.example.shop.entity.TableType;
import com.example.shop.exception.APIException;
import com.example.shop.payloads.CartDTO;
import com.example.shop.payloads.CartItemDTO;
import com.example.shop.payloads.TableTypeDTO;
import com.example.shop.repository.CartRepo;
import com.example.shop.repository.TableRepo;
import com.example.shop.repository.TableTypeRepo;
import com.example.shop.service.RestaurantTableService;
import com.example.shop.service.TypeTableService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-commerce Application")
public class TableTypeController {
    @Autowired
    TypeTableService typeTableService;
    @Autowired
    TableTypeRepo tableTypeRepo;
    @Autowired
    RestaurantTableService restaurantTableService;
    @PostMapping("/admin/tableType")
    public ResponseEntity<TableTypeDTO>createTableType(@Valid @RequestBody TableTypeDTO tableTypeDTO){
        TableTypeDTO AddTable=typeTableService.createTable(tableTypeDTO);
        return new ResponseEntity<>(AddTable, HttpStatus.CREATED);
    }
    @DeleteMapping("/admin/tableType/{tableId}")
    public ResponseEntity<String>delete(@PathVariable Long tableId){
        TableType tableType=tableTypeRepo.findById(tableId).orElseThrow(()->
                new APIException("Table type not found!!"));
        String deleted=typeTableService.deleted(tableId);
        return new ResponseEntity<>(deleted,HttpStatus.NO_CONTENT);
    }
    @GetMapping("/public/tables/enter")
    public ResponseEntity<CartDTO> enterTable(@RequestParam String token) {
        return ResponseEntity.ok(restaurantTableService.enterTableByToken(token));
    }
}

