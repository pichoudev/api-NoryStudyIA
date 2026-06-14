package com.norvya.norvya.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
            "attempts", "course", "user"})
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
            "courses", "subscriptions", "payments", "apiLogs", "passwordHash"})
    private User user;

    private int score;

    @Column(name = "total_questions")
    private int totalQuestions;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> answers;

    @Column(name = "time_spent_s")
    private int timeSpentSeconds;

    @Column(name = "attempted_at", updatable = false)
    private LocalDateTime attemptedAt;

    @PrePersist
    protected void onCreate() {
        this.attemptedAt = LocalDateTime.now();
    }
}