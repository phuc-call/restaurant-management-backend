package com.example.shop.repository;

import com.example.shop.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepo extends JpaRepository<OrderItem, Long> {
    @Query("""
                select oi
                from OrderItem oi
                where oi.order.id = :orderId
            """)
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);

    @Query("""
                SELECT oi
                FROM OrderItem oi
                JOIN FETCH oi.menuItem
                WHERE oi.order.id IN :orderIds
            """)
    List<OrderItem> findItemsByOrderIds(
            @Param("orderIds") List<Long> orderIds
    );

}
