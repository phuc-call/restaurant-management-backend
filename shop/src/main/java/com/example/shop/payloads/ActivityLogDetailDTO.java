package com.example.shop.payloads;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityLogDetailDTO {

    private Long id;
    private String entityType;
    private Long entityId;
    private String action;

    private String oldData;
    private String newData;

    private String performedBy;
    private String performerRole;

    private LocalDateTime performedAt;
}

