package com.norvya.norvya.service.websocket;

import com.norvya.norvya.entity.FlashcardProgress;
import com.norvya.norvya.repository.FlashcardProgressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlashcardReminderScheduler {

    private final FlashcardProgressRepository flashcardProgressRepository;
    private final NotificationService         notificationService;

    // ✅ Toutes les heures
    @Scheduled(fixedRate = 3600000)
    public void sendFlashcardReminders() {
        log.info("Vérification des rappels flashcards...");

        List<FlashcardProgress> dueCards = flashcardProgressRepository
                .findAllByNextReviewAtBefore(LocalDateTime.now());

        if (dueCards.isEmpty()) {
            log.info("Aucune carte à réviser");
            return;
        }

        // Grouper par email utilisateur puis par setId
        Map<String, Map<UUID, Long>> byUserAndSet = dueCards.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getUser().getEmail(),
                        Collectors.groupingBy(
                                p -> p.getFlashcardSet().getId(),
                                Collectors.counting()
                        )
                ));

        byUserAndSet.forEach((email, sets) ->
                sets.forEach((setId, count) -> {
                    notificationService.notifyFlashcardReminder(
                            email, setId, count.intValue()
                    );
                    log.info("Rappel → {} : {} carte(s) à réviser (set: {})",
                            email, count, setId);
                })
        );
    }
}