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
public class CourseResponse {
    private UUID id;
    private String title;
    private String language;
    private String fileUrl;
    private boolean hasContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer nombrePage;
    private String matiere;
}