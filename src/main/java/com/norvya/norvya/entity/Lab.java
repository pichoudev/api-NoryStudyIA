package com.norvya.norvya.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "labs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lab {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
            "summaries", "quizzes", "flashcardSets", "exercises", "labs", "user"})
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
            "courses", "subscriptions", "payments", "apiLogs", "passwordHash"})
    private User user;

    @Column(name = "scenario_title", nullable = false, length = 255)
    private String scenarioTitle;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> steps;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> objectives;

    @Column(name = "tokens_used")
    private int tokensUsed;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}