package com.norvya.norvya.service.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    // ── Génération terminée ────────────────────────────────
    public void notifyGenerationComplete(String userEmail,
                                         UUID courseId,
                                         String type,
                                         UUID contentId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type",        "GENERATION_COMPLETE");
        payload.put("courseId",    courseId.toString());
        payload.put("contentType", type);
        payload.put("contentId",   contentId.toString());
        payload.put("message",     type + " généré avec succès");
        payload.put("timestamp",   LocalDateTime.now().toString());

        sendToUser(userEmail, "/queue/notifications", payload);
        log.info("Notification génération — {} pour {}", type, userEmail);
    }

    // ── Génération échouée ─────────────────────────────────
    public void notifyGenerationError(String userEmail,
                                      UUID courseId,
                                      String type,
                                      String error) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type",        "GENERATION_ERROR");
        payload.put("courseId",    courseId.toString());
        payload.put("contentType", type);
        payload.put("message",     "Erreur lors de la génération de " + type);
        payload.put("error",       error != null ? error : "Erreur inconnue");
        payload.put("timestamp",   LocalDateTime.now().toString());

        sendToUser(userEmail, "/queue/notifications", payload);
        log.error("Notification erreur — {} pour {}", type, userEmail);
    }

    // ── Résultat quiz ──────────────────────────────────────
    public void notifyQuizResult(String userEmail,
                                 UUID quizId,
                                 int score,
                                 int total) {
        int percentage = total > 0 ? (score * 100) / total : 0;

        Map<String, Object> payload = new HashMap<>();
        payload.put("type",       "QUIZ_RESULT");
        payload.put("quizId",     quizId.toString());
        payload.put("score",      score);
        payload.put("total",      total);
        payload.put("percentage", percentage);
        payload.put("message",    "Quiz terminé — " + score + "/" + total
                + " (" + percentage + "%)");
        payload.put("timestamp",  LocalDateTime.now().toString());

        sendToUser(userEmail, "/queue/notifications", payload);
        log.info("Résultat quiz envoyé à {} — {}/{}", userEmail, score, total);
    }

    // ── Rappel révision flashcards ─────────────────────────
    public void notifyFlashcardReminder(String userEmail,
                                        UUID setId,
                                        int dueCount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type",      "FLASHCARD_REMINDER");
        payload.put("setId",     setId.toString());
        payload.put("dueCount",  dueCount);
        payload.put("message",   dueCount + " carte(s) à réviser maintenant");
        payload.put("timestamp", LocalDateTime.now().toString());

        sendToUser(userEmail, "/queue/notifications", payload);
        log.info("Rappel flashcards — {} cartes pour {}", dueCount, userEmail);
    }

    // ── Toutes générations terminées ───────────────────────
    public void notifyAllGenerationsComplete(String userEmail,
                                             UUID courseId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type",      "ALL_GENERATIONS_COMPLETE");
        payload.put("courseId",  courseId.toString());
        payload.put("message",   "Tout le contenu a été généré avec succès");
        payload.put("timestamp", LocalDateTime.now().toString());

        sendToUser(userEmail, "/queue/notifications", payload);
        log.info("Toutes générations terminées pour cours : {}", courseId);
    }

    // ── Broadcast général ──────────────────────────────────
    public void broadcast(String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type",      "BROADCAST");
        payload.put("message",   message);
        payload.put("timestamp", LocalDateTime.now().toString());

        messagingTemplate.convertAndSend("/topic/global", Optional.of(payload));
        log.info("Broadcast envoyé : {}", message);
    }

    // ── Helper ─────────────────────────────────────────────
    private void sendToUser(String email,
                            String destination,
                            Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(
                    email, destination, payload
            );
        } catch (Exception e) {
            log.error("Erreur envoi WebSocket à {} : {}",
                    email, e.getMessage());
        }
    }
}