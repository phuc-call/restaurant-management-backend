package com.example.shop.repository;

import com.example.shop.entity.ShiftGroup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftGroupRepo extends JpaRepository<ShiftGroup,Long> {
}
