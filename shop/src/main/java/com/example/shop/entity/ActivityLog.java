package com.example.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityType;
    private Long entityId;
    private String action;

    @Column(columnDefinition = "json")
    private String oldData;

    @Column(columnDefinition = "json")
    private String newData;

    private String performedBy;
    private String performerRole;

    private String customerName;
    private String customerPhone;

    private LocalDateTime performedAt;

    private String note;
}

