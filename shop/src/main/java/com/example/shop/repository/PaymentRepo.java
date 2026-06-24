package com.example.shop.repository;

import com.example.shop.entity.Order;
import com.example.shop.entity.Payment;
import com.example.shop.entity.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepo extends JpaRepository<Payment, Long> {
    @Query("""
                SELECT p FROM Payment p
                WHERE (:status IS NULL OR p.status = :status)
                  AND (
                       :keyword IS NULL
                       OR LOWER(p.table.tableName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                       OR CAST(p.table.tableId AS string) LIKE CONCAT('%', :keyword, '%')
                  )
            """)
    Page<Payment> searchPayments(
            @Param("status") PaymentStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("select p from Payment p where p.paymentId = :id")
//    Optional<Payment> findByIdForUpdate(@Param("id") Long id);
//
//    Optional<Payment> findByTable_TableIdAndStatus(
//            Long tableId,
//            PaymentStatus status
//    );

    //khác hàng còn sửa được
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                select p from Payment p
                where p.table.tableId = :tableId
                  and p.status = 'UNPAID'
                  and p.processing = false
            """)
    Optional<Payment> findEditablePayment(@Param("tableId") Long tableId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                select p from Payment p
                where p.paymentId = :paymentId
                  and p.status = 'UNPAID'
            """)
    Optional<Payment> findByIdForUpdate(@Param("paymentId") Long paymentId);

    @Query("""
                SELECT p
                FROM Payment p
                WHERE p.hidden = false
                  AND (:status IS NULL OR p.status = :status)
                  AND (:keyword IS NULL
                       OR LOWER(p.table.tableName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Payment> searchPaymentsForStaff(
            @Param("status") PaymentStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );


}
