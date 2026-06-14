package com.norvya.norvya.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttemptResponse {
    private UUID              id;
    private UUID              quizId;
    private int               score;
    private int               totalQuestions;
    private int               percentage;
    private Map<String,String> answers;
    private int               timeSpentSeconds;
    private LocalDateTime     attemptedAt;
}