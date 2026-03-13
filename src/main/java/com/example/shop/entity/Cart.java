package com.example.shop.entity;

import com.example.shop.entity.enums.ECartStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "carts", uniqueConstraints = @UniqueConstraint(columnNames = "table_id"))
public class Cart {

    @Id
    @Column(name = "cart_id")
    private Long id;
    @OneToOne
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable restaurantTable;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItem> cartItems;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ECartStatus status = ECartStatus.ACTIVE;

    private BigDecimal totalPrice = BigDecimal.ZERO;
    private LocalDateTime updatedAt;
    private String noteCart;

    private Boolean isHidden = false;

}
