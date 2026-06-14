package com.norvya.norvya.controller;

import com.norvya.norvya.dto.request.QuizAttemptRequest;
import com.norvya.norvya.dto.response.QuizAttemptResponse;
import com.norvya.norvya.dto.response.QuizResponse;
import com.norvya.norvya.entity.*;
import com.norvya.norvya.repository.*;
import com.norvya.norvya.service.generation.QuizService;
import com.norvya.norvya.service.storage.StorageService;
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
@RequestMapping("/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService           quizService;
    private final QuizRepository        quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final CourseRepository      courseRepository;
    private final UserRepository        userRepository;
    private final StorageService        storageService;

    // GET /api/quizzes/course/{courseId}
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<QuizResponse>> getByCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<QuizResponse> quizzes = quizRepository
                .findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(quizzes);
    }

    // GET /api/quizzes/{quizId}
    @GetMapping("/{quizId}")
    public ResponseEntity<QuizResponse> getById(
            @PathVariable UUID quizId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return quizRepository.findById(quizId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/quizzes/generate/{courseId}?questions=5
    @PostMapping("/generate/{courseId}")
    public ResponseEntity<QuizResponse> generate(
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = "5") int questions,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user     = getUser(userDetails);
        Course course = getCourse(courseId, user);
        byte[] pdfBytes = getPdfBytes(course);

        Quiz quiz = quizService.generateQuiz(
                course, user, questions,
                pdfBytes, course.getContentText()
        ).block();

        return ResponseEntity.ok(toResponse(quiz));
    }

    // POST /api/quizzes/{quizId}/attempt
    @PostMapping("/{quizId}/attempt")
    public ResponseEntity<QuizAttemptResponse> submitAttempt(
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

        return ResponseEntity.ok(toAttemptResponse(saved));
    }

    // GET /api/quizzes/{quizId}/attempts
    @GetMapping("/{quizId}/attempts")
    public ResponseEntity<List<QuizAttemptResponse>> getAttempts(
            @PathVariable UUID quizId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);

        List<QuizAttemptResponse> attempts = quizAttemptRepository
                .findByQuizIdAndUserOrderByAttemptedAtDesc(quizId, user)
                .stream()
                .map(this::toAttemptResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(attempts);
    }

    // DELETE /api/quizzes/{quizId}
    @DeleteMapping("/{quizId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID quizId,
            @AuthenticationPrincipal UserDetails userDetails) {

        quizRepository.deleteById(quizId);
        return ResponseEntity.noContent().build();
    }

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

    private QuizResponse toResponse(Quiz q) {
        return QuizResponse.builder()
                .id(q.getId())
                .courseId(q.getCourse().getId())
                .questions(q.getQuestions())
                .totalQuestions(q.getTotalQuestions())
                .tokensUsed(q.getTokensUsed())
                .createdAt(q.getCreatedAt())
                .build();
    }

    private QuizAttemptResponse toAttemptResponse(QuizAttempt a) {
        return QuizAttemptResponse.builder()
                .id(a.getId())
                .score(a.getScore())
                .totalQuestions(a.getTotalQuestions())
                .percentage(a.getTotalQuestions() > 0
                        ? (a.getScore() * 100) / a.getTotalQuestions() : 0)
                .timeSpentSeconds(a.getTimeSpentSeconds())
                .attemptedAt(a.getAttemptedAt())
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