package com.example.shop.controller;

import com.example.shop.payloads.OrderDTO;
import com.example.shop.payloads.RestaurantTableDTO;
import com.example.shop.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-commerce Application")
@RequiredArgsConstructor

public class OrderController {
    @Autowired
    OrderService orderService;
    // 1. Tạo Order từ table
    @PostMapping("public/order/{tableId}")
    public ResponseEntity<OrderDTO>createOrder(@PathVariable Long tableId) {
        return new ResponseEntity<>(orderService.createOrder(tableId), HttpStatus.CREATED) ;
    }

    // 2. Checkout order
    @PostMapping("/employee/manager/{orderId}")
    public ResponseEntity<String> checkout(@PathVariable Long orderId) {
         orderService.checkout(orderId);
         return new ResponseEntity<>(HttpStatus.OK);
    }
    // 3. Lấy order theo id
    @GetMapping("/employee/manager/{orderId}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long orderId) {
        return new ResponseEntity<>(orderService.getOrderById(orderId),HttpStatus.OK);
    }
}
