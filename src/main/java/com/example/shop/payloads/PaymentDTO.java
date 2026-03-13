package com.example.shop.payloads;

import com.example.shop.entity.enums.PaymentMethod;
import com.example.shop.entity.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDTO {
    private Long paymentId;      // ID yêu cầu thanh toán
    private Long tableId;        // ID bàn
    private String tableName;    // Tên bàn
    private Long cartId;         // ID cart
    private List<CartItemSnapshot> items; // Danh sách món (snapshot)
    private BigDecimal totalAmount; // Tổng tiền
    private PaymentStatus status;       // UNPAID
    private PaymentMethod method;
    private LocalDateTime updatedAt;
}
