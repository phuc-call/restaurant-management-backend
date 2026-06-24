package com.example.shop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MenuItem{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    @Size(max = 100)
    private String name;
    private boolean isException=true;
    @DecimalMin("0.0")
    private BigDecimal price;
    @Size(max = 500)
    private String description;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @OneToMany(
            mappedBy = "menuItem",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Image>images;
    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;


}
