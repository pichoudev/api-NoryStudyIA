package com.norvya.norvya.controller;

import com.norvya.norvya.dto.request.QuizAttemptRequest;
import com.norvya.norvya.dto.response.QuizAttemptResponse;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/quiz-attempts")
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRepository        quizRepository;
    private final UserRepository        userRepository;

    // ── Soumettre une tentative ────────────────────────────
    // POST /api/quiz-attempts/{quizId}
    @PostMapping("/{quizId}")
    public ResponseEntity<QuizAttemptResponse> submit(
            @PathVariable UUID quizId,
            @RequestBody QuizAttemptRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz introuvable"));

        int score = calculateScore(quiz, request);

        QuizAttempt attempt = QuizAttempt.builder()
                .quiz(quiz)
                .user(user)
                .score(score)
                .totalQuestions(quiz.getTotalQuestions())
                .answers(request.getAnswers())
                .timeSpentSeconds(request.getTimeSpentSeconds())
                .attemptedAt(LocalDateTime.now())
                .build();

        QuizAttempt saved = quizAttemptRepository.save(attempt);
        log.info("Tentative soumise — quiz : {}, score : {}/{}",
                quizId, score, quiz.getTotalQuestions());

        return ResponseEntity.ok(toResponse(saved));
    }

    // ── Historique des tentatives d'un quiz ────────────────
    // GET /api/quiz-attempts/quiz/{quizId}
    @GetMapping("/quiz/{quizId}")
    public ResponseEntity<List<QuizAttemptResponse>> getByQuiz(
            @PathVariable UUID quizId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        List<QuizAttemptResponse> attempts = quizAttemptRepository
                .findByQuizIdAndUserOrderByAttemptedAtDesc(quizId, user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(attempts);
    }

    // ── Toutes les tentatives de l'utilisateur ─────────────
    // GET /api/quiz-attempts/me
    @GetMapping("/me")
    public ResponseEntity<List<QuizAttemptResponse>> getMyAttempts(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        List<QuizAttemptResponse> attempts = quizAttemptRepository
                .findByUserOrderByAttemptedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(attempts);
    }

    // ── Récupérer une tentative par ID ─────────────────────
    // GET /api/quiz-attempts/{attemptId}
    @GetMapping("/{attemptId}")
    public ResponseEntity<QuizAttemptResponse> getById(
            @PathVariable UUID attemptId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return quizAttemptRepository.findById(attemptId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Statistiques d'un quiz ─────────────────────────────
    // GET /api/quiz-attempts/quiz/{quizId}/stats
    @GetMapping("/quiz/{quizId}/stats")
    public ResponseEntity<QuizStatsResponse> getStats(
            @PathVariable UUID quizId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        List<QuizAttempt> attempts = quizAttemptRepository
                .findByQuizIdAndUserOrderByAttemptedAtDesc(quizId, user);

        if (attempts.isEmpty()) {
            return ResponseEntity.ok(QuizStatsResponse.builder()
                    .quizId(quizId)
                    .totalAttempts(0)
                    .bestScore(0)
                    .averageScore(0.0)
                    .lastAttemptAt(null)
                    .build());
        }

        int bestScore = attempts.stream()
                .mapToInt(QuizAttempt::getScore)
                .max()
                .orElse(0);

        double averageScore = attempts.stream()
                .mapToInt(QuizAttempt::getScore)
                .average()
                .orElse(0.0);

        return ResponseEntity.ok(QuizStatsResponse.builder()
                .quizId(quizId)
                .totalAttempts(attempts.size())
                .bestScore(bestScore)
                .averageScore(Math.round(averageScore * 100.0) / 100.0)
                .lastAttemptAt(attempts.get(0).getAttemptedAt())
                .build());
    }

    // ── Supprimer une tentative ────────────────────────────
    // DELETE /api/quiz-attempts/{attemptId}
    @DeleteMapping("/{attemptId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID attemptId,
            @AuthenticationPrincipal UserDetails userDetails) {

        quizAttemptRepository.deleteById(attemptId);
        return ResponseEntity.noContent().build();
    }

    // ── Calculer le score ──────────────────────────────────
    @SuppressWarnings("unchecked")
    private int calculateScore(Quiz quiz, QuizAttemptRequest request) {
        int score = 0;
        for (Map<String, Object> question : quiz.getQuestions()) {
            String questionId = (String) question.get("id");
            String correct    = (String) question.get("correct");
            String userAnswer = request.getAnswers().get(questionId);
            if (correct != null && correct.equals(userAnswer)) score++;
        }
        return score;
    }

    // ── Mapper ─────────────────────────────────────────────
    private QuizAttemptResponse toResponse(QuizAttempt a) {
        return QuizAttemptResponse.builder()
                .id(a.getId())
                .quizId(a.getQuiz().getId())
                .score(a.getScore())
                .totalQuestions(a.getTotalQuestions())
                .percentage(a.getTotalQuestions() > 0
                        ? (a.getScore() * 100) / a.getTotalQuestions() : 0)
                .answers(a.getAnswers())
                .timeSpentSeconds(a.getTimeSpentSeconds())
                .attemptedAt(a.getAttemptedAt())
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
    public static class QuizStatsResponse {
        private UUID          quizId;
        private int           totalAttempts;
        private int           bestScore;
        private double        averageScore;
        private LocalDateTime lastAttemptAt;
    }
}