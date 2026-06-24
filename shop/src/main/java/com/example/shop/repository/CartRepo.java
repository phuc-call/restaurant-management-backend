package com.example.shop.repository;

import com.example.shop.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface CartRepo extends JpaRepository<Cart,Long> {
    Optional<Cart>findByRestaurantTable_tableId(Long tableId);
}
