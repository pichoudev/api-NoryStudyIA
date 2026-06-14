package com.norvya.norvya.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "flashcard_sets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashcardSet {

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

    // ✅ FlashcardSet.java
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> cards;  // Object pas String

    @Column(name = "total_cards")
    private int totalCards;

    @Column(name = "tokens_used")
    private int tokensUsed;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "flashcardSet", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "flashcardSet"})
    @Builder.Default
    private List<FlashcardProgress> progressList = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}