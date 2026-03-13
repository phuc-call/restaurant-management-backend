package com.example.shop.controller;

import com.example.shop.entity.enums.BookingStatus;
import com.example.shop.service.OverviewStaffService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@SecurityRequirement(name = "E-commerce Application")
@RestController
@RequestMapping("/api")
public class OverviewStaffController {
    @Autowired
    OverviewStaffService overviewStaffServicel;

    @GetMapping("/employee/staff/my-orders/count")
    public ResponseEntity<Long> countMyOrders() {
        Long total = overviewStaffServicel.countMyOrders();
        return ResponseEntity.ok(total);
    }
    @GetMapping("/employee/staff/booking/overview")
    public ResponseEntity<Map<BookingStatus, Long>> getMyBookingOverview() {
        return ResponseEntity.ok(overviewStaffServicel.getMyBookingOverview());
    }
    @GetMapping("/employee/staff/table-ordering/overview")
    public ResponseEntity<Long>getTableOrdering(){
        return ResponseEntity.ok(overviewStaffServicel.countOrderingTables());
    }

}
