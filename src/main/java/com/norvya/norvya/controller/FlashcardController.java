package com.norvya.norvya.controller;

import com.norvya.norvya.dto.request.FlashcardProgressRequest;
import com.norvya.norvya.dto.response.FlashcardProgressResponse;
import com.norvya.norvya.dto.response.FlashcardSetResponse;
import com.norvya.norvya.entity.*;
import com.norvya.norvya.repository.*;
import com.norvya.norvya.service.generation.FlashcardService;
import com.norvya.norvya.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/flashcards")
@RequiredArgsConstructor
public class FlashcardController {

    private final FlashcardService            flashcardService;
    private final FlashcardSetRepository      flashcardSetRepository;
    private final FlashcardProgressRepository flashcardProgressRepository;
    private final CourseRepository            courseRepository;
    private final UserRepository              userRepository;
    private final StorageService              storageService;

    // GET /api/flashcards/course/{courseId}
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<FlashcardSetResponse>> getByCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<FlashcardSetResponse> sets = flashcardSetRepository
                .findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(sets);
    }

    // GET /api/flashcards/{setId}
    @GetMapping("/{setId}")
    public ResponseEntity<FlashcardSetResponse> getById(
            @PathVariable UUID setId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return flashcardSetRepository.findById(setId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/flashcards/generate/{courseId}?cards=15
    @PostMapping("/generate/{courseId}")
    public ResponseEntity<FlashcardSetResponse> generate(
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = "15") int cards,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user     = getUser(userDetails);
        Course course = getCourse(courseId, user);
        byte[] pdfBytes = getPdfBytes(course);

        FlashcardSet set = flashcardService.generateFlashcards(
                course, user, cards,
                pdfBytes, course.getContentText()
        ).block();

        return ResponseEntity.ok(toResponse(set));
    }

    // PUT /api/flashcards/{setId}/progress
    @PutMapping("/{setId}/progress")
    public ResponseEntity<FlashcardProgressResponse> updateProgress(
            @PathVariable UUID setId,
            @RequestBody FlashcardProgressRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        FlashcardSet set = flashcardSetRepository.findById(setId)
                .orElseThrow(() -> new RuntimeException("Set introuvable"));

        FlashcardProgress progress = flashcardProgressRepository
                .findByFlashcardSetIdAndUserAndCardId(setId, user, request.getCardId())
                .orElse(FlashcardProgress.builder()
                        .flashcardSet(set)
                        .user(user)
                        .cardId(request.getCardId())
                        .reviewCount(0)
                        .status(FlashcardProgress.CardStatus.NEW)
                        .build());

        progress.setStatus(
                FlashcardProgress.CardStatus.valueOf(request.getStatus())
        );
        progress.setReviewCount(progress.getReviewCount() + 1);
        progress.setLastReviewedAt(LocalDateTime.now());
        progress.setNextReviewAt(calculateNextReview(request.getStatus()));

        FlashcardProgress saved = flashcardProgressRepository.save(progress);

        return ResponseEntity.ok(toProgressResponse(saved));
    }

    // GET /api/flashcards/{setId}/progress
    @GetMapping("/{setId}/progress")
    public ResponseEntity<List<FlashcardProgressResponse>> getProgress(
            @PathVariable UUID setId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        List<FlashcardProgressResponse> progress = flashcardProgressRepository
                .findByFlashcardSetIdAndUser(setId, user)
                .stream()
                .map(this::toProgressResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(progress);
    }

    // GET /api/flashcards/{setId}/due
    @GetMapping("/{setId}/due")
    public ResponseEntity<List<FlashcardProgressResponse>> getDueCards(
            @PathVariable UUID setId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        List<FlashcardProgressResponse> due = flashcardProgressRepository
                .findByFlashcardSetIdAndUserAndNextReviewAtBefore(
                        setId, user, LocalDateTime.now()
                )
                .stream()
                .map(this::toProgressResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(due);
    }

    // DELETE /api/flashcards/{setId}
    @DeleteMapping("/{setId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID setId,
            @AuthenticationPrincipal UserDetails userDetails) {

        flashcardSetRepository.deleteById(setId);
        return ResponseEntity.noContent().build();
    }

    private LocalDateTime calculateNextReview(String status) {
        return switch (status) {
            case "NEW"      -> LocalDateTime.now().plusDays(1);
            case "LEARNING" -> LocalDateTime.now().plusDays(3);
            case "MASTERED" -> LocalDateTime.now().plusDays(7);
            default         -> LocalDateTime.now().plusDays(1);
        };
    }

    private FlashcardSetResponse toResponse(FlashcardSet s) {
        return FlashcardSetResponse.builder()
                .id(s.getId())
                .courseId(s.getCourse().getId())
                .cards(s.getCards())
                .totalCards(s.getTotalCards())
                .tokensUsed(s.getTokensUsed())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private FlashcardProgressResponse toProgressResponse(FlashcardProgress p) {
        return FlashcardProgressResponse.builder()
                .id(p.getId())
                .cardId(p.getCardId())
                .status(p.getStatus().name())
                .reviewCount(p.getReviewCount())
                .nextReviewAt(p.getNextReviewAt())
                .lastReviewedAt(p.getLastReviewedAt())
                .build();
    }

    private byte[] getPdfBytes(Course course) {
        if (course.getFileUrl() != null) {
            try {
                return storageService.downloadFile(course.getFileUrl());
            } catch (Exception e) {
                log.warn("PDF non récupérable : {}", e.getMessage());
            }
        }
        return null;
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