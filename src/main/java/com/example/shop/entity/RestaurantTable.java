package com.example.shop.entity;

import com.example.shop.entity.enums.Floor;
import com.example.shop.entity.enums.TableStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "restaurant_table")
@Getter
@Setter
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "table_name", nullable = false, unique = true)
    private String tableName;

    @Min(value = 1, message = "Table number must be greater than zero")
    @Column(name = "number_table", nullable = false)
    private Long numberTable;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_type_id", nullable = false)
    private TableType tableType;

    @Min(1)
    @Column(name = "seat_count", nullable = false)
    private Long seatCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TableStatus status = TableStatus.AVAILABLE;

    @Column(nullable = false, unique = true)
    private String accessToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "floor", nullable = false)
    private Floor floor;


    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "restaurantTable", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] qrImage;
    private String qrUrl; // URL dùng để nhúng vào QR


    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

