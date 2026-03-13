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
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShiftGroup {
    @Id
    @GeneratedValue
    private Long id;

    private LocalDate workDate;

    @Enumerated(EnumType.STRING)
    private ShiftType shiftType;

    @ElementCollection
    private List<Floor> floors;

    private String note;

    private Long createdBy;

    private LocalDateTime createdAt;

    private boolean active = true;

    @OneToMany(mappedBy = "shiftGroup")
    private List<WorkShift> shifts;
}
