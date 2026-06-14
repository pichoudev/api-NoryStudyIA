package com.norvya.norvya.controller;

import com.norvya.norvya.dto.response.ExerciseResponse;
import com.norvya.norvya.entity.Course;
import com.norvya.norvya.entity.Exercise;
import com.norvya.norvya.entity.User;
import com.norvya.norvya.repository.CourseRepository;
import com.norvya.norvya.repository.ExerciseRepository;
import com.norvya.norvya.repository.UserRepository;
import com.norvya.norvya.service.generation.ExerciseService;
import com.norvya.norvya.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/exercises")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService    exerciseService;
    private final ExerciseRepository exerciseRepository;
    private final CourseRepository   courseRepository;
    private final UserRepository     userRepository;
    private final StorageService     storageService;

    // GET /api/exercises/course/{courseId}
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ExerciseResponse>> getByCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ExerciseResponse> exercises = exerciseRepository
                .findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(exercises);
    }

    // GET /api/exercises/{exerciseId}
    @GetMapping("/{exerciseId}")
    public ResponseEntity<ExerciseResponse> getById(
            @PathVariable UUID exerciseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return exerciseRepository.findById(exerciseId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/exercises/generate/{courseId}?problems=3
    @PostMapping("/generate/{courseId}")
    public ResponseEntity<ExerciseResponse> generate(
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = "3") int problems,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user     = getUser(userDetails);
        Course course = getCourse(courseId, user);
        byte[] pdfBytes = getPdfBytes(course);

        Exercise exercise = exerciseService.generateExercises(
                course, user, problems,
                pdfBytes, course.getContentText()
        ).block();

        return ResponseEntity.ok(toResponse(exercise));
    }

    // DELETE /api/exercises/{exerciseId}
    @DeleteMapping("/{exerciseId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID exerciseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        exerciseRepository.deleteById(exerciseId);
        return ResponseEntity.noContent().build();
    }

    private ExerciseResponse toResponse(Exercise e) {
        return ExerciseResponse.builder()
                .id(e.getId())
                .courseId(e.getCourse().getId())
                .problems(e.getProblems())
                .totalProblems(e.getTotalProblems())
                .tokensUsed(e.getTokensUsed())
                .createdAt(e.getCreatedAt())
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