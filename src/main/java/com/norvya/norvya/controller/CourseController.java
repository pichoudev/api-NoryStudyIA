package com.norvya.norvya.controller;

import com.norvya.norvya.dto.request.CourseRequest;
import com.norvya.norvya.dto.response.CourseResponse;
import com.norvya.norvya.dto.response.MessageResponse;
import com.norvya.norvya.entity.User;
import com.norvya.norvya.repository.UserRepository;
import com.norvya.norvya.service.course.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService  courseService;
    private final UserRepository userRepository;

    // ── Créer depuis texte ─────────────────────────────────
    // POST /api/courses/text
    @PostMapping("/text")
    public ResponseEntity<CourseResponse> createFromText(
            @Valid @RequestBody CourseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        return ResponseEntity.ok(
                courseService.createFromText(request, user)
        );
    }

    // ── Créer depuis PDF ───────────────────────────────────
    // POST /api/courses/upload
    @PostMapping(value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseResponse> createFromPdf(
            @RequestParam("title") String title,
            @RequestParam("nombre_page") Integer nombrePage,
            @RequestParam(value = "language", defaultValue = "fr") String language,
            @RequestParam("file") MultipartFile file,
            @RequestParam("matiere")  String matiere,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        return ResponseEntity.ok( 
                courseService.createFromPdf(title, language, file,nombrePage,matiere,user)
        );
    }

    // ── Générer tout le contenu ────────────────────────────
    // POST /api/courses/{courseId}/generate-all
    @PostMapping("/{courseId}/generate-all")
    public ResponseEntity<MessageResponse> generateAll(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        courseService.generateAllContent(courseId, user);
        return ResponseEntity.ok(
                new MessageResponse("Génération lancée en arrière-plan")
        );
    }

    // ── Lister les cours ───────────────────────────────────
    // GET /api/courses
    @GetMapping
    public ResponseEntity<List<CourseResponse>> getUserCourses(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        return ResponseEntity.ok(courseService.getUserCourses(user));
    }

    // ── Récupérer un cours ─────────────────────────────────
    // GET /api/courses/{courseId}
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponse> getCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        return ResponseEntity.ok(courseService.getCourse(courseId, user));
    }

    // ── Supprimer un cours ─────────────────────────────────
    // DELETE /api/courses/{courseId}
    @DeleteMapping("/{courseId}")
    public ResponseEntity<MessageResponse> deleteCourse(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = getUser(userDetails);
        courseService.deleteCourse(courseId, user);
        return ResponseEntity.ok(
                new MessageResponse("Cours supprimé avec succès")
        );
    }

    // ── Helper ─────────────────────────────────────────────
    private User getUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }
}