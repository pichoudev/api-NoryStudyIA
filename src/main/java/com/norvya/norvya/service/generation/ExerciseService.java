package com.norvya.norvya.service.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.norvya.norvya.entity.Course;
import com.norvya.norvya.entity.Exercise;
import com.norvya.norvya.entity.User;
import com.norvya.norvya.repository.ExerciseRepository;
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
public class ExerciseService {

    private final GeminiService      geminiService;
    private final ExerciseRepository exerciseRepository;
    private final PromptBuilder      promptBuilder;
    private final ObjectMapper       objectMapper;
    private final JsonRepairUtil     jsonRepairUtil;

    // ── Génération avec PDF natif ──────────────────────────
    public Mono<Exercise> generateExercises(Course course,
                                            User user,
                                            int numberOfProblems,
                                            byte[] pdfBytes,
                                            String textContent) {
        String prompt = """
            Génère %d exercices pratiques basés sur ce document.
            Varie les niveaux de difficulté (FACILE, MOYEN, DIFFICILE).
            Réponds UNIQUEMENT en JSON valide :
            {
              "problems": [
                {
                  "id": "p1",
                  "statement": "Énoncé de l'exercice",
                  "difficulty": "MOYEN",
                  "hints": ["Indice 1", "Indice 2"],
                  "solution": "Solution détaillée étape par étape"
                }
              ]
            }
        """.formatted(numberOfProblems);

        return geminiService.generateAuto(
                        prompt, pdfBytes, textContent, "EXERCISE", user)
                .map(json -> parseExercises(json, course, user))
                .map(exerciseRepository::save);
    }

    // ── Surcharge sans PDF ─────────────────────────────────
    public Mono<Exercise> generateExercises(Course course,
                                            User user,
                                            int numberOfProblems) {
        return generateExercises(course, user, numberOfProblems,
                null, course.getContentText());
    }

    @SuppressWarnings("unchecked")
    private Exercise parseExercises(String json, Course course, User user) {
        try {
            json = jsonRepairUtil.repair(json);
            Map<String, Object> data = objectMapper.readValue(json, Map.class);
            List<Map<String, Object>> problems =
                    (List<Map<String, Object>>) data.get("problems");

            if (problems == null || problems.isEmpty())
                throw new RuntimeException("Aucun exercice généré");

            return Exercise.builder()
                    .course(course)
                    .user(user)
                    .problems(problems)
                    .totalProblems(problems.size())
                    .tokensUsed(json.length() / 4)
                    .build();

        } catch (Exception e) {
            log.error("Erreur parsing exercices : {}", e.getMessage());
            throw new RuntimeException("Erreur parsing exercices : "
                    + e.getMessage());
        }
    }
}