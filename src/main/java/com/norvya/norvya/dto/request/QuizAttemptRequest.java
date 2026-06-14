package com.norvya.norvya.dto.request;

import lombok.Data;
import java.util.Map;

@Data
public class QuizAttemptRequest {
    private Map<String, String> answers;       // {questionId: reponse}
    private int timeSpentSeconds;
}