package com.example.shop.entity;

import com.example.shop.entity.enums.EOrderItem;
import com.example.shop.entity.enums.OrderStatus;
import com.example.shop.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "orderItem")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal totalPrice;
    private String noteOrder;
    private String nameTable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EOrderItem orderItemStatus;

}
