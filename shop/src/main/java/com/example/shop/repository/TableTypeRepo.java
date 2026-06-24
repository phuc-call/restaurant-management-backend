package com.example.shop.repository;

import com.example.shop.entity.TableType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableTypeRepo extends JpaRepository<TableType,Long> {
    boolean existsByNameIgnoreCase(String name);
}
