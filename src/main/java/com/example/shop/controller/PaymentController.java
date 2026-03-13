package com.example.shop.controller;

import com.example.shop.config.AppConstants;

import com.example.shop.entity.enums.ECartItemDeleteReason;
import com.example.shop.payloads.CartDTO;
import com.example.shop.payloads.CartItemDTO;
import com.example.shop.payloads.InvoiceDTO;

import com.example.shop.service.impl.PaymentServiceImpl;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
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

    @PostMapping("/employee/staff/{cartId}/pay-cash")
    public ResponseEntity<InvoiceDTO> payCash(
            @PathVariable Long cartId,
            @RequestParam BigDecimal cashReceived
    ) {
        return ResponseEntity.ok(
                paymentService.cashierPayCash(cartId, cashReceived)
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
    public ResponseEntity<Page<CartDTO>> getPayments(
            @RequestParam(defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = AppConstants.SORT_DIR) String sortOrder
    ) {
        return ResponseEntity.ok(
                paymentService.getPayments(
                        pageNumber,
                        pageSize,
                        sortBy,
                        sortOrder
                )
        );

    }

    @GetMapping("/employee/staff/payments/{paymentId}")
    public ResponseEntity<List<CartItemDTO>> getPaymentDetail(
            @PathVariable Long paymentId
    ) {
        return ResponseEntity.ok(
                paymentService.getPaymentDetail(paymentId)
        );
    }

    @PutMapping("/employee/staff/payments/{cartId}/cancel")
    public ResponseEntity<Void> cancelPayment(@PathVariable Long cartId,@RequestParam ECartItemDeleteReason reason) {
        paymentService.clearCartItemsAndAutoCancel(cartId,reason);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/vnpay/callback")
    public ResponseEntity<Void> vnpayCallback(
            @RequestParam("vnp_TxnRef") Long orderId,
            @RequestParam("vnp_ResponseCode") String responseCode
    ) {
        if ("00".equals(responseCode)) {
            paymentService.handleVnpaySuccess(orderId);
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/payment-success"))
                .build();
    }

    @PutMapping("/public/payment/{cartId}/request")
    public ResponseEntity<String> customerRequestToPay(@PathVariable Long cartId) {
        String dto = paymentService.customerRequestREADY_TO_PAY(cartId);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/employee/staff/payments/{cartId}/in-progress")
    public ResponseEntity<Map<String,String>> senToKetChen(@PathVariable Long cartId){
        paymentService.sendCartToKitchen(cartId);
        return ResponseEntity.ok(Map.of("message", "Đã gửi đơn xuống bếp thành công"));
    }



}