package com.norvya.norvya.service.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.norvya.norvya.entity.Course;
import com.norvya.norvya.entity.FlashcardSet;
import com.norvya.norvya.entity.User;
import com.norvya.norvya.repository.FlashcardSetRepository;
import com.norvya.norvya.util.JsonRepairUtil;
import com.norvya.norvya.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashcardService {

    private final GeminiService          geminiService;
    private final FlashcardSetRepository flashcardSetRepository;
    private final PromptBuilder          promptBuilder;
    private final ObjectMapper           objectMapper;
    private final JsonRepairUtil         jsonRepairUtil;

    // ── Génération avec PDF natif ──────────────────────────
    public Mono<FlashcardSet> generateFlashcards(Course course,
                                                 User user,
                                                 int numberOfCards,
                                                 byte[] pdfBytes,
                                                 String textContent) {
        String prompt = """
            Génère %d flashcards basées sur les concepts clés de ce document.
            Réponds UNIQUEMENT en JSON valide :
            {
              "cards": [
                {
                  "id": "f1",
                  "front": "Question ou terme",
                  "back": "Réponse ou définition concise"
                }
              ]
            }
        """.formatted(numberOfCards);

        return geminiService.generateAuto(
                        prompt, pdfBytes, textContent, "FLASHCARD", user)
                .map(json -> parseFlashcards(json, course, user))
                .map(flashcardSetRepository::save);
    }

    // ── Surcharge sans PDF ─────────────────────────────────
    public Mono<FlashcardSet> generateFlashcards(Course course,
                                                 User user,
                                                 int numberOfCards) {
        return generateFlashcards(course, user, numberOfCards,
                null, course.getContentText());
    }

    @SuppressWarnings("unchecked")
    private FlashcardSet parseFlashcards(String json,
                                         Course course, User user) {
        try {
            json = jsonRepairUtil.repair(json);
            Map<String, Object> data = objectMapper.readValue(json, Map.class);
// ✅ FlashcardService.java
            List<Map<String, Object>> cards =
                    (List<Map<String, Object>>) data.get("cards");

            if (cards == null || cards.isEmpty())
                throw new RuntimeException("Aucune flashcard générée");

            return FlashcardSet.builder()
                    .course(course)
                    .user(user)
                    .cards(cards)
                    .totalCards(cards.size())
                    .tokensUsed(json.length() / 4)
                    .build();

        } catch (Exception e) {
            log.error("Erreur parsing flashcards : {}", e.getMessage());
            throw new RuntimeException("Erreur parsing flashcards : "
                    + e.getMessage());
        }
    }
}