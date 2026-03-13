package com.example.shop.repository;

import com.example.shop.entity.RestaurantTable;

import com.example.shop.entity.enums.Floor;
import com.example.shop.entity.enums.TableStatus;

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
public interface RestaurantRepo extends JpaRepository<RestaurantTable, Long> {
    Optional<RestaurantTable> findByTableName(String tableName);

    Optional<RestaurantTable> findByNumberTable(Long numberTable);

    Optional<RestaurantTable> findByAccessToken(String token);

    @Query("""
            SELECT t FROM RestaurantTable t
            WHERE t.status <> com.example.shop.entity.enums.TableStatus.INACTIVE
              AND (:status IS NULL OR t.status = :status)
              AND (:floor IS NULL OR t.floor = :floor)
            """)
    Page<RestaurantTable> filter(
            @Param("status") TableStatus status,
            @Param("floor") Floor floor,
            Pageable pageable
    );


    Optional<RestaurantTable> findByTableIdAndAccessToken(Long tableId, String tableToken);

    @Query("""
            SELECT t FROM RestaurantTable t
            WHERE t.seatCount >= :guestCount
            AND t.status = com.example.shop.entity.enums.TableStatus.AVAILABLE
            AND t.tableId NOT IN (
                SELECT b.table.tableId FROM TableBooking b
                WHERE b.bookingTime BETWEEN :fromTime AND :toTime
            )
            ORDER BY t.seatCount ASC
            """)
    List<RestaurantTable> findAvailableTables(
            @Param("guestCount") int guestCount,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime
    );


    @Query("""
                SELECT t FROM RestaurantTable t
                WHERE t.seatCount >= :seatCount
                AND t.status = 'AVAILABLE'
                AND t.tableId NOT IN (
                    SELECT b.table.tableId FROM TableBooking b
                    WHERE b.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN')
                    AND ABS(
                        FUNCTION('TIMESTAMPDIFF', MINUTE, b.bookingTime, :bookingTime)
                    ) < 60
                )
                ORDER BY t.seatCount ASC
            """)
    List<RestaurantTable> findAvailableTablesForBooking(
            @Param("seatCount") Long seatCount,
            @Param("bookingTime") LocalDateTime bookingTime
    );

    @Query("""
            SELECT t FROM RestaurantTable t
            WHERE t.status = com.example.shop.entity.enums.TableStatus.INACTIVE
              AND (:floor IS NULL OR t.floor = :floor)
            """)
    Page<RestaurantTable> filterInactive(
            @Param("floor") Floor floor,
            Pageable pageable
    );


    @Query("""
                SELECT t.floor, t.tableName
                FROM RestaurantTable t
                WHERE t.status <> com.example.shop.entity.enums.TableStatus.INACTIVE
            
                ORDER BY t.floor, t.numberTable
            """)
    List<Object[]> findAllTableNamesWithFloor();

    @Query("""
            SELECT t.tableName
            FROM RestaurantTable t
            WHERE t.floor=:floor and t.status <> com.example.shop.entity.enums.TableStatus.INACTIVE
            ORDER BY t.numberTable""")
    List<String> findTableNameByFloor(@Param("floor") Floor floor);


}
