package com.example.shop.entity;

import com.example.shop.entity.enums.EReservation;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "reservations")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private User user;
    @ManyToOne
    private RestaurantTable restaurantTable;
    private LocalDateTime reserved_time;
    private Integer numberPeople;
    @Column(precision=10,scale = 2)
    private BigDecimal bookingFee;
    @Enumerated(EnumType.STRING)
    private EReservation status=EReservation.PENDING;
}
