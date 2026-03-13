package com.example.shop.controller;

import com.example.shop.entity.Order;
import com.example.shop.entity.enums.OrderStatus;
import com.example.shop.hellper.FileNameUtil;
import com.example.shop.payloads.HistoryOrderDTO;
import com.example.shop.payloads.KitchenOrderDTO;
import com.example.shop.payloads.OrderDTO;
import com.example.shop.payloads.ServiceOrderDTO;
import com.example.shop.repository.OrderRepo;
import com.example.shop.service.KitChenService;
import com.example.shop.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-commerce Application")
public class KitChenController {
    @Autowired
    KitChenService kitChenService;
    @Autowired
    OrderRepo orderRepo;

    @PutMapping("/employee/staff/orders/{orderId}/take")
    public ResponseEntity<?> takeOrder(@PathVariable Long orderId) {
        kitChenService.takeOrder(orderId);
        return ResponseEntity.ok(Map.of("message", "Nhận order thành công"));
    }

    @GetMapping("/employee/staff/orders/group-by-kitchen")
    public Map<String, List<KitchenOrderDTO>> getOrdersGroupedByKitchen() {
        return kitChenService.getOrdersGroupedByKitchen();
    }

    @PutMapping("/employee/staff/orders/{orderId}/done")
    public ResponseEntity<?> finishOrder(@PathVariable Long orderId) {

        kitChenService.finishOrder(orderId);

        return ResponseEntity.ok(
                Map.of(
                        "message", "Hoàn thành order thành công",
                        "orderId", orderId
                )
        );
    }

    @GetMapping("/employee/staff/kitchen/orders/done")
    public Page<KitchenOrderDTO> getDoneOrders(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "5") Integer size
    ) {
        return kitChenService.getDoneOrdersOfKitchen(page, size);
    }

    @GetMapping("/employee/staff/kitchen/orders/history")
    public Page<HistoryOrderDTO> getKitchenOrderHistory(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        return kitChenService.getKitchenOrderHistory(page, size);
    }

    @GetMapping("/employee/staff/kitchen/orders/history/export")
    public ResponseEntity<byte[]> exportKitchenHistoryExcel(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(required = false) String fileName
    ) throws IOException {

        List<HistoryOrderDTO> data =
                kitChenService.getKitchenHistoryForExport(from, to);

        byte[] excel =
                kitChenService.exportKitchenHistoryExcel(data);

        String finalFileName =
                FileNameUtil.buildExcelFileName(fileName);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + finalFileName + "\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(excel);
    }

    @PutMapping("/employee/staff/order-items/{orderItemId}/finish")
    public ResponseEntity<String> finishOrderItem(
            @PathVariable Long orderItemId
    ) {
        kitChenService.finishOrderItem(orderItemId);
        return ResponseEntity.ok().body(
                "Món ăn đã hoàn tất"
        );
    }

    @GetMapping("/employee/staff/{orderId}")
    public ResponseEntity<ServiceOrderDTO> getOrderDetail(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
                kitChenService.getOrderDetail(orderId)
        );
    }


    @PatchMapping("/admin/run")
    public ResponseEntity<?> runAutoAssignAndViewResult() {

        // chạy auto assign
        kitChenService.autoAssignKitchenForWaitingOrders();

        // xem kết quả sau khi chạy
        List<Order> pendingOrders = orderRepo.findByStatus(OrderStatus.PENDING);

        Map<Long, Integer> result = new HashMap<>();

        for (Order order : pendingOrders) {
            if (order.getKitchenStaff() == null) continue;

            Long userId = order.getKitchenStaff().getUserId();
            result.put(userId, result.getOrDefault(userId, 0) + 1);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/employee/staff/kitchen/my-order")
    public ResponseEntity<List<KitchenOrderDTO>>getMyOrders(){
        List<KitchenOrderDTO> orders =
                kitChenService.getOrderForPersonal();

        return ResponseEntity.ok(orders);
    }


}