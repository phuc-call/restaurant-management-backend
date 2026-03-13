package com.example.shop.entity;

import com.example.shop.entity.enums.EActivityResult;
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false)
    private String entityType;
    private Long entityId;
    private String action;

    @Column(columnDefinition = "LONGTEXT")
    private String oldData;

    @Column(columnDefinition = "LONGTEXT")
    private String newData;

    private String performedBy;

    @Column(nullable = false)
    private String performerRole;
    @Column(nullable = false)
    private String employeeName;
    private String customerPhone;
    @Column(nullable = false)
    private LocalDateTime performedAt;

    @Column(nullable = false)
    private String note;
    @Column(nullable = false)
    private EActivityResult result;

}

