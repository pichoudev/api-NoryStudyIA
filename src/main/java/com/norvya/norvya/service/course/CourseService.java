package com.norvya.norvya.service.course;

import com.norvya.norvya.dto.request.CourseRequest;
import com.norvya.norvya.dto.response.CourseResponse;
import com.norvya.norvya.entity.*;
import com.norvya.norvya.repository.CourseRepository;
import com.norvya.norvya.service.generation.*;
import com.norvya.norvya.service.storage.StorageService;
import com.norvya.norvya.service.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository    courseRepository;
    private final StorageService      storageService;
    private final SummaryService      summaryService;
    private final QuizService         quizService;
    private final FlashcardService    flashcardService;
    private final ExerciseService     exerciseService;
    private final LabService          labService;
    private final NotificationService notificationService;

    // ── Créer cours depuis texte ───────────────────────────
    @Transactional
    public CourseResponse createFromText(CourseRequest request, User user) {
        Course course = Course.builder()
                .user(user)
                .title(request.getTitle())
                .language(request.getLanguage())
                .contentText(request.getContentText())
                .build();

        courseRepository.save(course);
        log.info("Cours créé depuis texte : {}", course.getId());
        return toResponse(course);
    }

    // ── Créer cours depuis PDF ─────────────────────────────
    @Transactional
    public CourseResponse createFromPdf(String title,
                                        String language,
                                        MultipartFile file,
                                        Integer nombrePage,
                                        String matiere,
                                        User user) {
        validatePdf(file);

        try {
            byte[] pdfBytes = file.getBytes();
            String fileUrl  = storageService.uploadFile(file, "courses");

            Course course = Course.builder()
                    .user(user)
                    .title(title)
                    .language(language != null ? language : "fr")
                    .contentText(null)
                    .fileUrl(fileUrl)
                    .nombrePage(nombrePage)
                    .matiere(matiere)
                    .pdfBytes(pdfBytes)
                    .build();

            courseRepository.save(course);
            log.info("Cours créé depuis PDF : {} ({} octets)",
                    course.getId(), pdfBytes.length);

            return toResponse(course);

        } catch (Exception e) {
            throw new RuntimeException("Erreur création cours PDF : "
                    + e.getMessage());
        }
    }

    // ── Générer tout le contenu ────────────────────────────
    @Transactional
    public void generateAllContent(UUID courseId, User user) {

        Course course = courseRepository.findByIdAndUser(courseId, user)
                .orElseThrow(() -> new RuntimeException("Cours introuvable"));

        log.info("Génération complète — cours : {}", courseId);

        byte[] pdfBytes   = null;
        String textContent = course.getContentText();
        String userEmail   = user.getEmail();

        if (course.getFileUrl() != null) {
            try {
                log.info("Téléchargement PDF depuis R2...");
                pdfBytes = storageService.downloadFile(course.getFileUrl());
                log.info("PDF récupéré — {} octets", pdfBytes.length);
            } catch (Exception e) {
                log.warn("PDF non récupérable : {}", e.getMessage());
            }
        }

        final byte[] finalPdfBytes = pdfBytes;
        final String finalText     = textContent;

        // Compteur pour savoir quand toutes les générations sont terminées
        AtomicInteger completedCount  = new AtomicInteger(0);
        AtomicInteger errorCount      = new AtomicInteger(0);
        final int     totalGenerations = 5;

        // ✅ Quiz
        quizService.generateQuiz(course, user, 5, finalPdfBytes, finalText)
                .doOnSuccess(q -> {
                    log.info("✅ Quiz généré");
                    notificationService.notifyGenerationComplete(
                            userEmail, courseId, "QUIZ", q.getId()
                    );
                    checkAllDone(userEmail, courseId,
                            completedCount, errorCount, totalGenerations);
                })
                .doOnError(e -> {
                    log.error("❌ Quiz : {}", e.getMessage());
                    notificationService.notifyGenerationError(
                            userEmail, courseId, "QUIZ", e.getMessage()
                    );
                    errorCount.incrementAndGet();
                    checkAllDone(userEmail, courseId,
                            completedCount, errorCount, totalGenerations);
                })
                .subscribe();

        // ✅ Flashcards
        flashcardService.generateFlashcards(
                        course, user, 15, finalPdfBytes, finalText)
                .doOnSuccess(f -> {
                    log.info("✅ Flashcards générées");
                    notificationService.notifyGenerationComplete(
                            userEmail, courseId, "FLASHCARD", f.getId()
                    );
                    checkAllDone(userEmail, courseId,
                            completedCount, errorCount, totalGenerations);
                })
                .doOnError(e -> {
                    log.error("❌ Flashcards : {}", e.getMessage());
                    notificationService.notifyGenerationError(
                            userEmail, courseId, "FLASHCARD", e.getMessage()
                    );
                    errorCount.incrementAndGet();
                    checkAllDone(userEmail, courseId,
                            completedCount, errorCount, totalGenerations);
                })
                .subscribe();

        // ✅ Exercices
        exerciseService.generateExercises(
                        course, user, 3, finalPdfBytes, finalText)
                .doOnSuccess(e -> {
                    log.info("✅ Exercices générés");
                    notificationService.notifyGenerationComplete(
                            userEmail, courseId, "EXERCISE", e.getId()
                    );
                    checkAllDone(userEmail, courseId,
                            completedCount, errorCount, totalGenerations);
                })
                .doOnError(e -> {
                    log.error("❌ Exercices : {}", e.getMessage());
                    notificationService.notifyGenerationError(
                            userEmail, courseId, "EXERCISE", e.getMessage()
                    );
                    errorCount.incrementAndGet();
                    checkAllDone(userEmail, courseId,
                            completedCount, errorCount, totalGenerations);
                })
                .subscribe();

        // ✅ Lab
        labService.generateLab(course, user, finalPdfBytes, finalText)
                .doOnSuccess(l -> {
                    log.info("✅ Lab généré");
                    notificationService.notifyGenerationComplete(
                            userEmail, courseId, "LAB", l.getId()
                    );
                    checkAllDone(userEmail, courseId,
                            completedCount, errorCount, totalGenerations);
                })
                .doOnError(e -> {
                    log.error("❌ Lab : {}", e.getMessage());
                    notificationService.notifyGenerationError(
                            userEmail, courseId, "LAB", e.getMessage()
                    );
                    errorCount.incrementAndGet();
                    checkAllDone(userEmail, courseId,
                            completedCount, errorCount, totalGenerations);
                })
                .subscribe();

        // ✅ Résumé en dernier — le plus lourd
        summaryService.generateSummary(
                        course, user, finalPdfBytes, finalText)
                .doOnSuccess(s -> {
                    log.info("✅ Résumé généré");
                    notificationService.notifyGenerationComplete(
                            userEmail, courseId, "SUMMARY", s.getId()
                    );
                    checkAllDone(userEmail, courseId,
                            completedCount, errorCount, totalGenerations);
                })
                .doOnError(e -> {
                    log.error("❌ Résumé : {}", e.getMessage());
                    notificationService.notifyGenerationError(
                            userEmail, courseId, "SUMMARY", e.getMessage()
                    );
                    errorCount.incrementAndGet();
                    checkAllDone(userEmail, courseId,
                            completedCount, errorCount, totalGenerations);
                })
                .subscribe();
    }

    // ── Vérifier si toutes les générations sont terminées ──
    private void checkAllDone(String userEmail,
                              UUID courseId,
                              AtomicInteger completed,
                              AtomicInteger errors,
                              int total) {
        int done = completed.incrementAndGet();
        if (done >= total) {
            if (errors.get() == 0) {
                notificationService.notifyAllGenerationsComplete(
                        userEmail, courseId
                );
            } else {
                log.warn("{} erreur(s) sur {} générations", errors.get(), total);
            }
        }
    }

    // ── Lister les cours ───────────────────────────────────
    public List<CourseResponse> getUserCourses(User user) {
        return courseRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Récupérer un cours ─────────────────────────────────
    public CourseResponse getCourse(UUID courseId, User user) {
        Course course = courseRepository.findByIdAndUser(courseId, user)
                .orElseThrow(() -> new RuntimeException("Cours introuvable"));
        return toResponse(course);
    }

    // ── Supprimer un cours ─────────────────────────────────
    @Transactional
    public void deleteCourse(UUID courseId, User user) {
        Course course = courseRepository.findByIdAndUser(courseId, user)
                .orElseThrow(() -> new RuntimeException("Cours introuvable"));

        if (course.getFileUrl() != null)
            storageService.deleteFile(course.getFileUrl());

        courseRepository.delete(course);
        log.info("Cours supprimé : {}", courseId);
    }

    // ── Helpers ────────────────────────────────────────────
    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Le fichier est vide");
        if (!"application/pdf".equals(file.getContentType()))
            throw new IllegalArgumentException("Seuls les PDFs sont acceptés");
        if (file.getSize() > 20 * 1024 * 1024)
            throw new IllegalArgumentException("Fichier max 20 Mo");
    }

    private CourseResponse toResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .language(course.getLanguage())
                .fileUrl(course.getFileUrl())
                .hasContent(course.getFileUrl() != null
                        || (course.getContentText() != null
                        && !course.getContentText().isEmpty()))
                .createdAt(course.getCreatedAt())
                .build();
    }
}