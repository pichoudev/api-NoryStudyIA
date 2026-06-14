package com.norvya.norvya.dto.request;

import lombok.Data;

@Data
public class FlashcardProgressRequest {
    private String cardId;
    private String status;  // NEW, LEARNING, MASTERED
}