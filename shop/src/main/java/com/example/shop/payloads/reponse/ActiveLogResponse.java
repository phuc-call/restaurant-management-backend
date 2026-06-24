package com.example.shop.payloads.reponse;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
public class ActiveLogResponse {
    private Long id;

    private String entityType;
    private Long entityId;
    private String action;
    private String oldData;
    private String newData;

    private String performedBy;
    private String performerRole;
    private String customerName;
    private String customerPhone;

    private LocalDateTime performedAt;

    private String note;
}
