package com.norvya.norvya.controller;

import com.norvya.norvya.dto.request.FlashcardProgressRequest;
import com.norvya.norvya.dto.response.FlashcardProgressResponse;
import com.norvya.norvya.entity.*;
import com.norvya.norvya.repository.*;
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
@RequestMapping("/flashcard-progress")
@RequiredArgsConstructor
public class FlashcardProgressController {

    private final FlashcardProgressRepository flashcardProgressRepository;
    private final FlashcardSetRepository      flashcardSetRepository;
    private final UserRepository              userRepository;

    // ── Mettre à jour la progression d'une carte ───────────
    // PUT /api/flashcard-progress/{setId}/card
    @PutMapping("/{setId}/card")
    public ResponseEntity<FlashcardProgressResponse> updateCard(
            @PathVariable UUID setId,
            @RequestBody FlashcardProgressRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        FlashcardSet set = flashcardSetRepository.findById(setId)
                .orElseThrow(() -> new RuntimeException("Set introuvable"));

        FlashcardProgress progress = flashcardProgressRepository
                .findByFlashcardSetIdAndUserAndCardId(
                        setId, user, request.getCardId()
                )
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

        log.info("Progression mise à jour — carte : {}, statut : {}",
                request.getCardId(), request.getStatus());

        return ResponseEntity.ok(toResponse(saved));
    }

    // ── Récupérer la progression d'un set ─────────────────
    // GET /api/flashcard-progress/{setId}
    @GetMapping("/{setId}")
    public ResponseEntity<List<FlashcardProgressResponse>> getBySet(
            @PathVariable UUID setId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        List<FlashcardProgressResponse> progress = flashcardProgressRepository
                .findByFlashcardSetIdAndUser(setId, user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(progress);
    }

    // ── Cartes à réviser maintenant ────────────────────────
    // GET /api/flashcard-progress/{setId}/due
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
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(due);
    }

    // ── Statistiques de progression d'un set ──────────────
    // GET /api/flashcard-progress/{setId}/stats
    @GetMapping("/{setId}/stats")
    public ResponseEntity<FlashcardStatsResponse> getStats(
            @PathVariable UUID setId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        List<FlashcardProgress> allProgress = flashcardProgressRepository
                .findByFlashcardSetIdAndUser(setId, user);

        FlashcardSet set = flashcardSetRepository.findById(setId)
                .orElseThrow(() -> new RuntimeException("Set introuvable"));

        long newCount = allProgress.stream()
                .filter(p -> p.getStatus() == FlashcardProgress.CardStatus.NEW)
                .count();

        long learningCount = allProgress.stream()
                .filter(p -> p.getStatus() == FlashcardProgress.CardStatus.LEARNING)
                .count();

        long masteredCount = allProgress.stream()
                .filter(p -> p.getStatus() == FlashcardProgress.CardStatus.MASTERED)
                .count();

        long dueCount = flashcardProgressRepository
                .findByFlashcardSetIdAndUserAndNextReviewAtBefore(
                        setId, user, LocalDateTime.now()
                ).size();

        int totalCards  = set.getTotalCards();
        int masteredPct = totalCards > 0
                ? (int) (masteredCount * 100 / totalCards) : 0;

        return ResponseEntity.ok(FlashcardStatsResponse.builder()
                .setId(setId)
                .totalCards(totalCards)
                .newCount((int) newCount)
                .learningCount((int) learningCount)
                .masteredCount((int) masteredCount)
                .dueCount((int) dueCount)
                .masteredPercentage(masteredPct)
                .build());
    }

    // ── Réinitialiser la progression d'un set ─────────────
    // DELETE /api/flashcard-progress/{setId}/reset
    @DeleteMapping("/{setId}/reset")
    public ResponseEntity<FlashcardStatsResponse> reset(
            @PathVariable UUID setId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        List<FlashcardProgress> progress = flashcardProgressRepository
                .findByFlashcardSetIdAndUser(setId, user);

        flashcardProgressRepository.deleteAll(progress);

        log.info("Progression réinitialisée pour le set : {}", setId);

        FlashcardSet set = flashcardSetRepository.findById(setId)
                .orElseThrow(() -> new RuntimeException("Set introuvable"));

        return ResponseEntity.ok(FlashcardStatsResponse.builder()
                .setId(setId)
                .totalCards(set.getTotalCards())
                .newCount(set.getTotalCards())
                .learningCount(0)
                .masteredCount(0)
                .dueCount(0)
                .masteredPercentage(0)
                .build());
    }

    // ── Calculer la prochaine révision ─────────────────────
    private LocalDateTime calculateNextReview(String status) {
        return switch (status) {
            case "NEW"      -> LocalDateTime.now().plusDays(1);
            case "LEARNING" -> LocalDateTime.now().plusDays(3);
            case "MASTERED" -> LocalDateTime.now().plusDays(7);
            default         -> LocalDateTime.now().plusDays(1);
        };
    }

    // ── Mapper ─────────────────────────────────────────────
    private FlashcardProgressResponse toResponse(FlashcardProgress p) {
        return FlashcardProgressResponse.builder()
                .id(p.getId())
                .cardId(p.getCardId())
                .status(p.getStatus().name())
                .reviewCount(p.getReviewCount())
                .nextReviewAt(p.getNextReviewAt())
                .lastReviewedAt(p.getLastReviewedAt())
                .build();
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    // ── DTO Stats interne ──────────────────────────────────
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FlashcardStatsResponse {
        private UUID setId;
        private int  totalCards;
        private int  newCount;
        private int  learningCount;
        private int  masteredCount;
        private int  dueCount;
        private int  masteredPercentage;
    }
}