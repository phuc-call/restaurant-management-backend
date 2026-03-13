package com.example.shop.hellper;

import com.example.shop.payloads.reponse.EmployeeScheduleResponse;
import com.example.shop.payloads.reponse.UserShiftDTO;

public class AISchedule {
    public static String buildContext(
            EmployeeScheduleResponse hasSchedule,
            EmployeeScheduleResponse noSchedule
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("DANH SÁCH NHÂN VIÊN ĐÃ CÓ LỊCH LÀM:\n");
        if (hasSchedule.getContent().isEmpty()) {
            sb.append("- Không có nhân viên nào\n");
        } else {
            hasSchedule.getContent().forEach(e -> {
                sb.append("- ")
                        .append(e.getUserName())
                        .append(" (")
                        .append(e.getEmail())
                        .append(")\n");
            });
        }

        sb.append("\nDANH SÁCH NHÂN VIÊN CHƯA CÓ LỊCH:\n");
        if (noSchedule.getContent().isEmpty()) {
            sb.append("- Không có nhân viên nào\n");
        } else {
            noSchedule.getContent().forEach(e -> {
                sb.append("- ")
                        .append(e.getUserName())
                        .append(" (")
                        .append(e.getEmail())
                        .append(")\n");
            });
        }

        return sb.toString();
    }


}
