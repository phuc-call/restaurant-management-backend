package com.example.shop.repository;

import com.example.shop.entity.Position;
import com.example.shop.entity.enums.EPositionType;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface PositionRepo extends JpaRepository<Position,Long> {
    Optional<Position> findByName(EPositionType name);
}
