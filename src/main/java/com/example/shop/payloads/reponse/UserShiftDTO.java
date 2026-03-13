package com.example.shop.payloads.reponse;

import com.example.shop.entity.enums.Floor;
import com.example.shop.entity.enums.ShiftType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor

@Getter
@Setter
public class UserShiftDTO {
    private Long userId;
    private String userName;
    private String email;
    private LocalDate workDate;
    private ShiftType shiftType;
    private List<Floor> floors;
    private String note;
    public UserShiftDTO(
            Long userId,
            String userName,
            String email,
            LocalDate workDate,
            ShiftType shiftType,
            String note
    ) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.workDate = workDate;
        this.shiftType = shiftType;
        this.note = note;
    }
}
