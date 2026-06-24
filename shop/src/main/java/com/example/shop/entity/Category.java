package com.example.shop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "menu_category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;
    @NotBlank
    @Column(unique = true)
    private String name;

    private String description;

    @OneToMany(mappedBy = "category",cascade = CascadeType.REMOVE,orphanRemoval = true,fetch = FetchType.LAZY) // ✅ trùng với Image.category
    private List<Image>images;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private List<MenuItem> items;
}
