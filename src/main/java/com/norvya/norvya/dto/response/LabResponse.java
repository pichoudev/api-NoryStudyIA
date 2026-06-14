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
public class LabResponse {
    private UUID                      id;
    private UUID                      courseId;
    private String                    scenarioTitle;
    private List<String>              objectives;
    private List<Map<String, Object>> steps;
    private int                       tokensUsed;
    private LocalDateTime             createdAt;
}