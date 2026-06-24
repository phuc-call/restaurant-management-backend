package com.example.shop.entity;

import com.example.shop.entity.enums.PaymentMethod;
import com.example.shop.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String cartSnapshot;
    @Enumerated(EnumType.STRING)
    @Column(name = "method", length = 20)
    private PaymentMethod method;
    @Enumerated(EnumType.STRING)
    @Column(name = "static", length = 20)
    private PaymentStatus status;

    @Column(nullable = false)
    private boolean hidden=false;

    @ManyToOne
    @JoinColumn(nullable = false, name = "table_id")
    private RestaurantTable table;

    @Column(name = "cash_received")
    private BigDecimal cashReceived;

    @Version
    private Long version;
    @Column(nullable = false)
    private Boolean processing = false;

    @Column(name = "change_amount")
    private BigDecimal changeAmount;


    @Column(nullable = false, updatable = false)
    private LocalDateTime createAt = LocalDateTime.now();
    @Column(nullable = false, updatable = false)
    private String updateBy;
    @Column(nullable = false, updatable = false)
    private String updatedRole;
    private LocalDateTime updateAt;
}
