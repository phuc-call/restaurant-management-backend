package com.example.shop.repository;

import com.example.shop.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepo extends JpaRepository<ActivityLog,Long> {
    @Query("""
                SELECT a FROM ActivityLog a
                WHERE (:entityType IS NULL OR a.entityType = :entityType)
                  AND (:action IS NULL OR a.action = :action)
                  AND (:performerRole IS NULL OR a.performerRole = :performerRole)
                  AND (:fromDate IS NULL OR a.performedAt >= :fromDate)
                  AND (:toDate IS NULL OR a.performedAt <= :toDate)
          
            """)
    Page<ActivityLog> findLogs(
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("performerRole") String performerRole,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );


    @Modifying
    @Query("""
                DELETE FROM ActivityLog a
                WHERE a.performedAt < :beforeDate
            """)
    int deleteLogsBefore(@Param("beforeDate") LocalDateTime beforeDate);

    @Modifying
    @Query("""
                DELETE FROM ActivityLog a
                WHERE a.entityType = :entityType
                  AND a.entityId = :entityId
            """)
    int deleteByEntity(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId
    );

    @Query("""
        SELECT a
        FROM ActivityLog a
        WHERE a.user.userId = :userId
          AND (:entityType IS NULL OR a.entityType = :entityType)
          AND (
                :keyword IS NULL OR
                LOWER(a.action) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(a.note) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        ORDER BY a.performedAt DESC
    """)
    Page<ActivityLog> findMyActivities(
            @Param("userId") Long userId,
            @Param("entityType") String entityType,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
    SELECT a
    FROM ActivityLog a
    WHERE a.user.userId = :userId
      AND (:entityType IS NULL OR a.entityType = :entityType)
      AND a.performedAt BETWEEN :fromDate AND :toDate
    ORDER BY a.performedAt DESC
""")
    List<ActivityLog> findMyActivitiesForExport(
            @Param("userId") Long userId,
            @Param("entityType") String entityType,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

}
