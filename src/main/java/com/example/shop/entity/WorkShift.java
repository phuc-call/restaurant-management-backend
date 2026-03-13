package com.example.shop.entity;

import com.example.shop.entity.enums.Floor;
import com.example.shop.entity.enums.ShiftType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_shift",uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "work_date", "shift_type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long workShiftId;

    // Nhân viên
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User staff;

    // Ngày làm việc
    @Column(nullable = false)
    private LocalDate workDate;

    // Ca làm
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShiftType shiftType=ShiftType.MORNING;

    @ElementCollection(targetClass = Floor.class)
@CollectionTable(
        name = "work_shift_floor",
        joinColumns = @JoinColumn(name = "workShiftId")
)
    @Enumerated(EnumType.STRING)
    @Column(name = "floor")
    private List<Floor>floors=new ArrayList<>();
    // Thời gian thực tế
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;

    @ManyToOne
    @JoinColumn(name = "shift_group_id")
    private ShiftGroup shiftGroup;

    // Ghi chú
    private String note;

    @Column(nullable = false)
    private boolean active = true;
}

