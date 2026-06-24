package com.example.shop.payloads;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityLogResponseDTO {

    private Long id;

    private String entityType;
    private Long entityId;
    private String action;

    private String performedBy;
    private String performerRole;

    private String customerName;
    private String customerPhone;

    private LocalDateTime performedAt;

    private String note;
}
