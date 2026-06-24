package com.example.shop.repository;

import com.example.shop.entity.MenuItem;
import com.example.shop.payloads.MenuItemDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface MenuItemRepo extends JpaRepository<MenuItem, Long> {

    // SỬA LỖI: dùng đúng tên field trong Category (categoryId)
    List<MenuItem> findByCategory_CategoryId(Long categoryId);

    boolean existsByNameIgnoreCase(String name);

    // FILTER
    @Query("""
        SELECT m FROM MenuItem m
        WHERE (:categoryId IS NULL OR m.category.categoryId = :categoryId)
          AND (:keyword IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:minPrice IS NULL OR m.price >= :minPrice)
          AND (:maxPrice IS NULL OR m.price <= :maxPrice)
    """)
    Page<MenuItem> filter(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}
