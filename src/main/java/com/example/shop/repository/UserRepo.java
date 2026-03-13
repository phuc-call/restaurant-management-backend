package com.example.shop.repository;

import com.example.shop.entity.User;
import com.example.shop.entity.enums.EAccountStatus;
import com.example.shop.payloads.reponse.UserShiftDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends ShareRepo<User, Long> {
    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.addresses
            WHERE u.email=:email
            """)
    Optional<User> findUserWithAddresses(String email);

    Optional<User> findByEmail(String email);

    // Lấy user theo role
    @Query("""
                SELECT DISTINCT u
                FROM User u
                JOIN u.roles r
                WHERE r.roleName = :roleName
            """)
    Page<User> findByRoleName(
            @Param("roleName") String roleName,
            Pageable pageable
    );

    // Lấy tất cả user có ít nhất 1 role
    @Query("""
                SELECT DISTINCT u
                FROM User u
                JOIN u.roles r
            """)
    Page<User> findAllUsersWithRole(Pageable pageable);

    @Query("""
            SELECT u
            FROM User u
            JOIN u.roles r
            WHERE u.userId=:userId AND r.roleName<>'USER'""")
    Optional<User> findByIdAndRoleNotUser(@Param("userId") Long userId);


    // nhân viên có lịch trong khoảng thời gian
    @Query("""
                SELECT DISTINCT u
                FROM User u
                JOIN WorkShift w ON w.staff.userId = u.userId
                WHERE w.workDate BETWEEN :from AND :to
            """)
    Page<User> findEmployeesWithSchedule(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable
    );

    // ===== NHÂN VIÊN ĐÃ CÓ LỊCH =====
    @Query("""
                SELECT new com.example.shop.payloads.reponse.UserShiftDTO(
                    u.userId,
                    u.userName,
                    u.email,
                    w.workDate,
                    w.shiftType,
                    w.note
                )
                FROM WorkShift w
                JOIN w.staff u
                WHERE w.workDate BETWEEN :from AND :to
                  AND (:userName IS NULL
                       OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :userName, '%')))
            """)
    Page<UserShiftDTO> findUserShiftsFlat(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("userName") String userName,
            Pageable pageable
    );


    // ===== NHÂN VIÊN CHƯA CÓ LỊCH =====
    @Query(
            value = """
                                        SELECT new com.example.shop.payloads.reponse.UserShiftDTO(
                                            u.userId,
                                            u.userName,
                                            u.email,
                                            NULL,
                                            NULL,
                                            NULL
                                        )
                                        FROM WorkShift w
                    JOIN w.staff u
                                        WHERE NOT EXISTS (
                                            SELECT 1
                                            FROM WorkShift w
                                            WHERE w.staff = u
                                              AND w.workDate BETWEEN :from AND :to
                                        )
                                        AND (:userName IS NULL
                                             OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :userName, '%')))
                    """,
            countQuery = """
                        SELECT COUNT(u)
                        FROM User u
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM WorkShift w
                            WHERE w.staff = u
                              AND w.workDate BETWEEN :from AND :to
                        )
                        AND (:userName IS NULL
                             OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :userName, '%')))
                    """
    )

    Page<UserShiftDTO> findEmployeesWithoutScheduleDTO(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("userName") String userName,
            Pageable pageable
    );
    @Query("""
            SELECT DISTINCT u
             FROM User u
             JOIN u.positions p
            WHERE p.name = com.example.shop.entity.enums.EPositionType.KITCHEN
                                             AND u.accountStatus = com.example.shop.entity.enums.EAccountStatus.ACTIVE
""")
    List<User> findActiveKitchenStaff();
    List<User>findByAccountStatusAndRoles_RoleName(EAccountStatus status, String roleName);


}
