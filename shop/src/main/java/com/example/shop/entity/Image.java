package com.example.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "images")
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageUrl;
    private String altText;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menuImage",nullable = true)
    private MenuItem menuItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tableImage",nullable = true)
    private RestaurantTable restaurantTable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoryMenuImage",nullable = true)
    private Category category;
}
