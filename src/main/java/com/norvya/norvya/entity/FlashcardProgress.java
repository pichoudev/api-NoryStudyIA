package com.norvya.norvya.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "flashcard_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashcardProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flashcard_set_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
            "progressList", "course", "user"})
    private FlashcardSet flashcardSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler",
            "courses", "subscriptions", "payments", "apiLogs", "passwordHash"})
    private User user;

    @Column(name = "card_id", nullable = false)
    private String cardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CardStatus status = CardStatus.NEW;

    @Column(name = "review_count")
    @Builder.Default
    private int reviewCount = 0;

    @Column(name = "next_review_at")
    private LocalDateTime nextReviewAt;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    public enum CardStatus {
        NEW, LEARNING, MASTERED
    }
}