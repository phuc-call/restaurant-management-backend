package com.example.shop.repository;

import com.example.shop.entity.RestaurantTable;
import com.example.shop.entity.enums.TableStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TableRepo extends JpaRepository<RestaurantTable, Long> {
    Optional<RestaurantTable> findByAccessToken(String token);


}
