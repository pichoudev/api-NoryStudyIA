package com.norvya.norvya.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResponse {
    private UUID                     id;
    private UUID                     courseId;
    private List<Map<String, Object>> sections;
    private List<String>             keyPoints;
    private List<Map<String, Object>> glossary;
    private int                      tokensUsed;
    private LocalDateTime            createdAt;
}