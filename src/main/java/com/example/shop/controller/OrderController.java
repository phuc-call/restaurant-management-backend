package com.example.shop.controller;

import com.example.shop.payloads.*;
import com.example.shop.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-commerce Application")
@RequiredArgsConstructor

public class OrderController {
    @Autowired
    OrderService orderService;

    // 1. Tạo Order từ table
    @PostMapping("public/order/{tableId}")
    public ResponseEntity<OrderDTO> createOrder(@PathVariable Long tableId) {
        return new ResponseEntity<>(orderService.createOrder(tableId), HttpStatus.CREATED);
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
        return new ResponseEntity<>(orderService.getOrderById(orderId), HttpStatus.OK);
    }

    //xuất bill
    @GetMapping("/employee/staff/orders/{orderId}/bill")
    public ResponseEntity<byte[]> exportBill(@PathVariable Long orderId) {

        byte[] pdf = orderService.generateBillPdf(orderId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=bill-" + orderId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
    @PostMapping("/employee/staff/orders/pay-merged-carts")
    public ResponseEntity<InvoiceDTO> payMergedCarts(
            @RequestBody PayMergedCartsRequestDTO request
    ) {
        return ResponseEntity.ok(orderService.payMergedCarts(request));
    }

    @GetMapping("/employee/staff/orders/waiting")
    public ResponseEntity<List<KitchenOrderDTO>> getKitchenOrders() {
        return ResponseEntity.ok(orderService.getOrdersForKitchen());
    }
    @GetMapping("/employee/staff/orders/pending")
    public ResponseEntity<List<KitchenOrderDTO>> getKitchenOrdersOfStaff() {
        return ResponseEntity.ok(orderService.getOrdersOfKitchen());
    }
}
