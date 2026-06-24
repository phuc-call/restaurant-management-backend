package com.example.shop.repository;

import com.example.shop.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionRepo extends JpaRepository<Position,Long> {
    List<Position>findByName(String name);
}
