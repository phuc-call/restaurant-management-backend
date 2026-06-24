package com.example.shop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "tableTypes")
public class TableType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;
    @Column(nullable = false)
    @Size(message = "The name type of table can not be null")
    private String name;
    @OneToMany(mappedBy = "tableType")
    private List<RestaurantTable> tables = new ArrayList<>();
    @Size(max = 255)
    private String description;
    @NotNull(message = "The number of seat have to better zero")
    private Long seatCount;
    @NotNull(message = "surcharge can not be null!!")
    @Min(value = 0, message = "Invalid surcharge")
    @Max(value = 100, message = "Maximum surcharge is 100%")
    private Double extraFee; // % phụ thu
}
