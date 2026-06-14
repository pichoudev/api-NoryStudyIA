package com.norvya.norvya.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // nullable — certains appels sont système

    @Column(name = "generation_type", nullable = false, length = 20)
    private String generationType;  // SUMMARY, QUIZ, FLASHCARD, EXERCISE, LAB

    @Column(name = "tokens_input")
    private int tokensInput;

    @Column(name = "tokens_output")
    private int tokensOutput;

    @Column(name = "latency_ms")
    private int latencyMs;

    @Column(nullable = false, length = 20)
    private String status;  // SUCCESS, ERROR, TIMEOUT, RATE_LIMITED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}