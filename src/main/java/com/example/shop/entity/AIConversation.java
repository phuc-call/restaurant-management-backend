package com.example.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "ai_conversation")
public class AIConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long aiId;
    private String sessionId;
    @Column(columnDefinition = "TEXT")
    private String lastQuestion;
    @Column(columnDefinition = "TEXT")
    private String lastAnswer;
    private LocalDateTime updatedAt;
}
