package com.example.shop.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "addresses")
public class Address{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id") // <- tên column thật trong MySQL
    private Long addressId;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String street;   // số nhà + tên đường
    private String ward;     // phường/xã
    private String district; // quận/huyện
    private String city;     // thành phố/tỉnh
    private String country;  // quốc gia
    @Size(max = 6000, message = "Ghi chú tối đa 1000 từ")
    @Column(columnDefinition = "TEXT")
    private String note;
    public Address(String street,String ward,String district,String city,String country,String note){
        this.street=street;
        this.ward=ward;
        this.district=district;
        this.city=city;
        this.country=country;

        this.note=note;
    }
}
