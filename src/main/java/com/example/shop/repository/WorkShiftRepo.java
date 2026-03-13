package com.example.shop.repository;

import com.example.shop.entity.WorkShift;
import com.example.shop.entity.enums.ShiftType;
import com.example.shop.payloads.WorkShiftDTO;
import com.example.shop.payloads.reponse.WorkShiftOverview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

import java.util.List;

@Repository
public interface WorkShiftRepo extends JpaRepository<WorkShift, Long> {
    boolean existsByStaff_UserIdAndWorkDateAndShiftType(
            Long userId,
            LocalDate workDate,
            ShiftType shiftType
    );

    List<WorkShiftDTO> findByStaff_UserIdAndWorkDateBetween(
            Long userId,
            LocalDate from,
            LocalDate to
    );

    @Query(
            "SELECT DISTINCT ws " +
                    "FROM WorkShift ws " +
                    "JOIN FETCH ws.staff s " +
                    "LEFT JOIN FETCH ws.floors f " +
                    "WHERE s.id = :userId " +
                    "AND ws.workDate BETWEEN :from AND :to"
    )
    List<WorkShift> findMyScheduleEntity(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
                SELECT w
                FROM WorkShift w
                JOIN FETCH w.floors
                WHERE w.staff.userId = :userId
                  AND w.workDate = :workDate
                  AND w.shiftType = :shiftType
            """)
    WorkShift findShiftWithFloors(
            @Param("userId") Long userId,
            @Param("workDate") LocalDate workDate,
            @Param("shiftType") ShiftType shiftType
    );

    // lấy tổng nhân viên
    @Query("""
            SELECT new com.example.shop.payloads.reponse.WorkShiftOverview(
                ws.staff.userId,
                ws.staff.userName,
                SUM(CASE WHEN ws.shiftType='MORNING' THEN 1 ELSE 0 END),
                SUM(CASE WHEN ws.shiftType='AFTERNOON' THEN 1 ELSE 0 END),
                SUM(CASE WHEN ws.shiftType='EVENING' THEN 1 ELSE 0 END),
                COUNT(ws)
            )
            FROM WorkShift ws
            WHERE ws.workDate BETWEEN :from AND :to
            AND ws.active = true
            GROUP BY ws.staff.userId, ws.staff.userName
            """)
    List<WorkShiftOverview> getShiftStatistic(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    List<WorkShift>findByWorkDate(LocalDate date);
    List<WorkShift>findByWorkDateBetween(LocalDate from, LocalDate to);
}

