package com.norvya.norvya.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardProgressResponse {
    private UUID          id;
    private String        cardId;
    private String        status;
    private int           reviewCount;
    private LocalDateTime nextReviewAt;
    private LocalDateTime lastReviewedAt;
}