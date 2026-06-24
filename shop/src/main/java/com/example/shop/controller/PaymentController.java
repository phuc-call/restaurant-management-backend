package com.example.shop.controller;

import com.example.shop.entity.Payment;
import com.example.shop.entity.enums.PaymentStatus;
import com.example.shop.payloads.InvoiceDTO;

import com.example.shop.payloads.PaymentDTO;
import com.example.shop.service.impl.PaymentServiceImpl;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@SecurityRequirement(name = "E-commerce Application")
@RestController
@RequestMapping("/api")
public class PaymentController {
    @Autowired
    PaymentServiceImpl paymentService;

    @PostMapping("/cart/{tableId}/vnpay")
    public ResponseEntity<?> payByVnpay(
            @PathVariable Long tableId,
            @RequestParam String token
    ) {
        String payUrl = paymentService.payCartByVnpay(tableId, token);
        return ResponseEntity.ok(Map.of("payUrl", payUrl));
    }

    @GetMapping("/vnpay/return")
    public String vnpayReturn(@RequestParam Map<String, String> params) {

        String responseCode = params.get("vnp_ResponseCode");
        String paymentId = params.get("vnp_TxnRef");

        if ("00".equals(responseCode)) {
            paymentService.confirmMockMomo(Long.valueOf(paymentId));
            return "THANH TOAN THANH CONG";
        }
        return "THANH TOAN THAT BAI";
    }

    @PostMapping("/mock/momo/confirm/{paymentId}")
    public void confirmMockMomo(@PathVariable Long paymentId) {
        paymentService.confirmMockMomo(paymentId);
    }

    @PostMapping("/employee/staff/{paymentId}/pay-cash")
    public ResponseEntity<InvoiceDTO> payCash(
            @PathVariable Long paymentId,
            @RequestParam BigDecimal cashReceived
    ) {
        return ResponseEntity.ok(
                paymentService.cashierPayCash(paymentId, cashReceived)
        );
    }

    @PostMapping("/public/payment/{tableId}")
    public ResponseEntity<Void> customerCashRequest(
            @PathVariable Long tableId
    ) {
        paymentService.customerCashRequest(tableId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/employee/staff/payments")
    public ResponseEntity<Page<PaymentDTO>> getPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "updateAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order
    ) {
        return ResponseEntity.ok(
                paymentService.getPayments(
                        status,
                        keyword,
                        pageNumber,
                        pageSize,
                        sortBy,
                        order
                )
        );

    }

    @GetMapping("/employee/staff/payments/{paymentId}")
    public ResponseEntity<PaymentDTO> getPaymentDetail(
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(
                paymentService.getPaymentDetail(paymentId)
        );
    }

    @PutMapping("/employee/staff/payments/{paymentId}/cancel")
    public ResponseEntity<Void> cancelPayment(@PathVariable Long paymentId) {
        paymentService.hidePayment(paymentId);
        return ResponseEntity.ok().build();
    }

}