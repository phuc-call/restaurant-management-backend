package com.example.shop.entity;

import com.example.shop.entity.enums.BookingStatus;
import com.example.shop.entity.enums.CancelReason;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
@Table(name = "table_booking")
public class TableBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable table;
    @NotBlank(message = "Tên khách không được để trống")
    @Column(nullable = false)
    private String customerName;
    @NotBlank(message = "Số điện thoại không được để trống")
    @Column(nullable = false)
    private String customerPhone;
    @Column(nullable = false)
    private LocalDateTime bookingTime;   // thời gian khách đến
    private LocalDateTime endTime;        // optional

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;
    // PENDING | CONFIRMED | CANCELLED | NO_SHOW | COMPLETED
    @NotNull(message = "Vui lòng nhập số lượng khách")
    @Min(value = 1, message = "Số lượng khách phải lớn hơn 0")
    @Column(nullable = false)
    private Integer numberOfGuests;

    private String note;
    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason")
    private CancelReason reason;

    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "update_by")
    private String updateBy;
    @Column(name = "update_at")
    private LocalDateTime updateAt;
    @Column(name = "updated_role")
    private String updatedRole;
}
