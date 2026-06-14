package com.norvya.norvya.controller;

import com.norvya.norvya.dto.response.LabResponse;
import com.norvya.norvya.entity.Course;
import com.norvya.norvya.entity.Lab;
import com.norvya.norvya.entity.User;
import com.norvya.norvya.repository.CourseRepository;
import com.norvya.norvya.repository.LabRepository;
import com.norvya.norvya.repository.UserRepository;
import com.norvya.norvya.service.generation.LabService;
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
@RequestMapping("/labs")
@RequiredArgsConstructor
public class LabController {

    private final LabService       labService;
    private final LabRepository    labRepository;
    private final CourseRepository courseRepository;
    private final UserRepository   userRepository;
    private final StorageService   storageService;

    // GET /api/labs/course/{courseId}
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<LabResponse>> getByCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<LabResponse> labs = labRepository
                .findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(labs);
    }

    // GET /api/labs/{labId}
    @GetMapping("/{labId}")
    public ResponseEntity<LabResponse> getById(
            @PathVariable UUID labId,
            @AuthenticationPrincipal UserDetails userDetails) {

        return labRepository.findById(labId)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/labs/generate/{courseId}
    @PostMapping("/generate/{courseId}")
    public ResponseEntity<LabResponse> generate(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user     = getUser(userDetails);
        Course course = getCourse(courseId, user);
        byte[] pdfBytes = getPdfBytes(course);

        Lab lab = labService.generateLab(
                course, user,
                pdfBytes, course.getContentText()
        ).block();

        return ResponseEntity.ok(toResponse(lab));
    }

    // DELETE /api/labs/{labId}
    @DeleteMapping("/{labId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID labId,
            @AuthenticationPrincipal UserDetails userDetails) {

        labRepository.deleteById(labId);
        return ResponseEntity.noContent().build();
    }

    private LabResponse toResponse(Lab l) {
        return LabResponse.builder()
                .id(l.getId())
                .courseId(l.getCourse().getId())
                .scenarioTitle(l.getScenarioTitle())
                .objectives(l.getObjectives())
                .steps(l.getSteps())
                .tokensUsed(l.getTokensUsed())
                .createdAt(l.getCreatedAt())
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