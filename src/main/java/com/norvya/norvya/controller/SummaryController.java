package com.norvya.norvya.controller;

import com.norvya.norvya.dto.response.SummaryResponse;
import com.norvya.norvya.entity.Course;
import com.norvya.norvya.entity.Summary;
import com.norvya.norvya.entity.User;
import com.norvya.norvya.repository.CourseRepository;
import com.norvya.norvya.repository.SummaryRepository;
import com.norvya.norvya.repository.UserRepository;
import com.norvya.norvya.service.generation.SummaryService;
import com.norvya.norvya.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/summaries")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService    summaryService;
    private final SummaryRepository summaryRepository;
    private final CourseRepository  courseRepository;
    private final UserRepository    userRepository;
    private final StorageService    storageService;

    // GET /api/summaries/course/{courseId}
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<SummaryResponse>> getByCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<SummaryResponse> summaries = summaryRepository
                .findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(summaries);
    }

    // GET /api/summaries/{summaryId}
    @GetMapping("/{summaryId}")
    public ResponseEntity<SummaryResponse> getById(
            @PathVariable UUID summaryId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return summaryRepository.findById(summaryId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/summaries/generate/{courseId}
    @PostMapping("/generate/{courseId}")
    public ResponseEntity<SummaryResponse> generate(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user     = getUser(userDetails);
        Course course = getCourse(courseId, user);

        byte[] pdfBytes = null;
        if (course.getFileUrl() != null) {
            try {
                pdfBytes = storageService.downloadFile(course.getFileUrl());
            } catch (Exception e) {
                log.warn("PDF non récupérable : {}", e.getMessage());
            }
        }

        Summary summary = summaryService.generateSummary(
                course, user, pdfBytes, course.getContentText()
        ).block();

        return ResponseEntity.ok(toResponse(summary));
    }

    // GET /api/summaries/generate/{courseId}/stream
    @GetMapping(
            value = "/generate/{courseId}/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> generateStream(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user     = getUser(userDetails);
        Course course = getCourse(courseId, user);
        return summaryService.generateSummaryStream(course, user);
    }

    // DELETE /api/summaries/{summaryId}
    @DeleteMapping("/{summaryId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID summaryId,
            @AuthenticationPrincipal UserDetails userDetails) {

        summaryRepository.deleteById(summaryId);
        return ResponseEntity.noContent().build();
    }

    // ── Mapper ─────────────────────────────────────────────
    private SummaryResponse toResponse(Summary s) {
        return SummaryResponse.builder()
                .id(s.getId())
                .courseId(s.getCourse().getId())
                .sections(s.getSections())
                .keyPoints(s.getKeyPoints())
                .glossary(s.getGlossary())
                .tokensUsed(s.getTokensUsed())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    private Course getCourse(UUID courseId, User user) {
        return courseRepository.findByIdAndUser(courseId, user)
                .orElseThrow(() -> new RuntimeException("Cours introuvable"));
    }
}