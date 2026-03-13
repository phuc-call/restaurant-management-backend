package com.example.shop.repository;

import com.example.shop.entity.Order;
import com.example.shop.entity.enums.OrderStatus;

import com.example.shop.payloads.KitchenOrderFlatDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepo extends JpaRepository<Order, Long> {
    @Query("""
            select o from Order o
            left join fetch o.orderItems oi
            left join fetch oi.menuItem
            where o.id = :orderId
            """)
    Optional<Order> findByIdWithItems(Long orderId);

    @Query("""
                SELECT COUNT(o)
                FROM Order o
                WHERE o.customer.id = :userId
            """)
    Long countOrdersByUserId(
            @Param("userId") Long userId
    );

    @Query("""
                SELECT new com.example.shop.payloads.KitchenOrderFlatDTO(
                    o.id,
                    o.billCode,
                    t.tableName,
                    o.noteOrder,
                    ks.userName,
                    oi.id,
                    mi.name,
                    oi.quantity,
                    oi.nameTable,
                    oi.noteOrder,
                    oi.orderItemStatus
                )
                FROM Order o
                JOIN o.orderItems oi
                JOIN oi.menuItem mi
                JOIN o.table t
                LEFT JOIN o.kitchenStaff ks
                WHERE o.status IN :statuses
                  AND o.hidden = false
            """)
    List<KitchenOrderFlatDTO> findKitchenOrdersFlat(
            @Param("statuses") List<OrderStatus> statuses
    );

    @Query("""
                SELECT DISTINCT o
                FROM Order o
                JOIN FETCH o.kitchenStaff ks
                JOIN FETCH o.orderItems oi
                JOIN FETCH oi.menuItem mi
                JOIN FETCH o.table t
                WHERE o.status = :status
                  AND o.kitchenStaff IS NOT NULL
                  AND o.hidden = false
            """)
    List<Order> findAllKitchenWorkingOrders(
            @Param("status") OrderStatus status
    );

    @Query("""
                SELECT o
                FROM Order o
                WHERE o.status = :status
                  AND o.kitchenStaff.id = :kitchenStaffId
                  AND o.hidden = false
                  AND o.updatedAt >= :fromTime
            """)
    Page<Order> findDoneOrdersOfKitchenStaff(
            @Param("kitchenStaffId") Long kitchenStaffId,
            @Param("status") OrderStatus status,
            @Param("fromTime") LocalDateTime fromTime,
            Pageable pageable
    );

    @Query("""
                SELECT o
                FROM Order o
                WHERE o.status = :status
                  AND o.kitchenStaff.id = :kitchenStaffId
                  AND o.hidden = false
            """)
    Page<Order> findKitchenOrderHistory(
            @Param("kitchenStaffId") Long kitchenStaffId,
            @Param("status") OrderStatus status,
            Pageable pageable
    );

    @Query("""
                select o
                from Order o
                where o.kitchenStaff.id = :userId
                  and o.status = :status
                  and o.createdAt between :fromTime and :toTime
                order by o.createdAt desc
            """)
    List<Order> findKitchenHistoryForExport(
            @Param("userId") Long userId,
            @Param("status") OrderStatus status,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime
    );

    @Query("""
                SELECT COUNT(o)
                FROM Order o
                WHERE o.kitchenStaff.id = :userId
                  AND o.status IN (:statuses)
            """)
    long countActiveOrdersByKitchen(
            @Param("userId") Long userId,
            @Param("statuses") List<OrderStatus> statuses
    );

    //order chưa có nhân viên và trạng thái là waiting
    @Query("""
                SELECT o
                FROM Order o
                WHERE o.status = :status
                  AND o.kitchenStaff IS NULL
                  AND o.hidden = false
                ORDER BY o.createdAt ASC
            """)
    List<Order> findWaitingOrdersForAutoAssign(
            @Param("status") OrderStatus status,
            Pageable pageable
    );
List<Order>findByStatus(OrderStatus status);

    @Query("""
    SELECT new com.example.shop.payloads.KitchenOrderFlatDTO(
        o.id,
        o.billCode,
        t.tableName,
        o.noteOrder,
        ks.userName,
        oi.id,
        mi.name,
        oi.quantity,
        oi.nameTable,
        oi.noteOrder,
        oi.orderItemStatus
    )
    FROM Order o
    JOIN o.orderItems oi
    JOIN oi.menuItem mi
    JOIN o.table t
    JOIN o.kitchenStaff ks
    WHERE o.kitchenStaff.id = :userId
      AND o.status = :status
      AND o.hidden = false
""")
    List<KitchenOrderFlatDTO> findPersonalKitchenOrdersFlat(
            @Param("userId") Long userId,
            @Param("status") OrderStatus status
    );
}



