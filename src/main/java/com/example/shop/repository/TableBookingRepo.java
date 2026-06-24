package com.example.shop.repository;


import com.example.shop.entity.TableBooking;
import com.example.shop.entity.enums.BookingStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;



public interface TableBookingRepo extends JpaRepository<TableBooking, Long> {
    @Query("SELECT b FROM TableBooking b WHERE b.status='CONFIRMED' AND b.bookingTime < :expiredTime")
    List<TableBooking> findExpiredConfirmedBookings(
            @Param("expiredTime") LocalDateTime expiredTime
    );

    @Query(
            value = """
                    SELECT *
                    FROM table_booking b
                    WHERE (:minGuests IS NULL OR b.number_of_guests >= :minGuests)
                      AND (:maxGuests IS NULL OR b.number_of_guests <= :maxGuests)
                      AND (:fromTime IS NULL OR b.booking_time >= :fromTime)
                      AND (:toTime IS NULL OR b.booking_time <= :toTime)
                      AND (:customerPhone IS NULL OR b.customer_phone = :customerPhone)
                      AND (
                        IFNULL(:customerName, '') = ''
                        OR LOWER(b.customer_name) LIKE CONCAT('%', LOWER(:customerName), '%')
                      )
                    
                    ORDER BY(b.booking_time<CURRENT_TIMESTAMP) ASC, ABS(TIMESTAMPDIFF(MINUTE,b.booking_time,CURRENT_TIMESTAMP))ASC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM table_booking b
                    WHERE (:minGuests IS NULL OR b.number_of_guests >= :minGuests)
                      AND (:maxGuests IS NULL OR b.number_of_guests <= :maxGuests)
                      AND (:fromTime IS NULL OR b.booking_time >= :fromTime)
                      AND (:toTime IS NULL OR b.booking_time <= :toTime)
                      AND (:customerPhone IS NULL OR b.customer_phone = :customerPhone)
                      AND (
                                    IFNULL(:customerName, '') = ''
                                    OR LOWER(b.customer_name) LIKE CONCAT('%', LOWER(:customerName), '%')
                                  )
                    
                    """,
            nativeQuery = true
    )
    Page<TableBooking> searchBookings(
            @Param("minGuests") Integer minGuests,
            @Param("maxGuests") Integer maxGuests,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("customerPhone") String customerPhone,
            @Param("customerName") String customerName,
            Pageable pageable
    );

    List<TableBooking> findByStatusOrderByBookingTimeAsc(BookingStatus status);

    List<TableBooking> findByTable_TableNameOrderByBookingTimeAsc(String tableName);


    boolean existsByTable_TableIdAndStatusInAndBookingTimeBetween(//TODO: watch again
                                                                  Long tableId,
                                                                  List<BookingStatus> statuses,
                                                                  LocalDateTime start,
                                                                  LocalDateTime end
    );

    List<TableBooking> findByCustomerPhoneOrderByBookingTimeDesc(String customerPhone);

    @Query("""
            SELECT DISTINCT t.status
            FROM TableBooking t
            """)
    List<BookingStatus> findAllStaus();

    @Query("""
            SELECT b.status,COUNT(b)
            FROM TableBooking b
            GROUP BY b.status
            """)
    List<Object[]> getBookingGroupByStatus();



    @Query("""
                SELECT b.status, COUNT(b)
                FROM TableBooking b
                WHERE b.updatedByUser.id = :userId
                GROUP BY b.status
            """)
    List<Object[]> getBookingGroupByStatusAndUser(
            @Param("userId") Long userId
    );
    Boolean existsByCustomerPhone(String customerPhone);

    boolean existsByTable_TableIdAndStatusIn(
            Long tableId,
            List<BookingStatus> statuses
    );
    List<TableBooking>findByCustomerPhone(String phone);
}
