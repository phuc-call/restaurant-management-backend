package com.example.shop.repository;


import com.example.shop.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TableRepo extends JpaRepository<RestaurantTable, Long> {
    Optional<RestaurantTable> findByAccessToken(String token);
@Query("""
        SELECT COUNT(t) FROM RestaurantTable t WHERE t.status=com.example.shop.entity.enums.TableStatus.ORDERING""")
Long countTablesOrdering();
}
