package com.norvya.norvya.service.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.norvya.norvya.entity.Course;
import com.norvya.norvya.entity.Quiz;
import com.norvya.norvya.entity.User;
import com.norvya.norvya.repository.QuizRepository;
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
public class QuizService {

    private final GeminiService  geminiService;
    private final QuizRepository quizRepository;
    private final PromptBuilder  promptBuilder;
    private final ObjectMapper   objectMapper;
    private final JsonRepairUtil jsonRepairUtil;

    // ── Génération avec PDF natif ──────────────────────────
    public Mono<Quiz> generateQuiz(Course course,
                                   User user,
                                   int numberOfQuestions,
                                   byte[] pdfBytes,
                                   String textContent) {
        String prompt = """
            Génère %d questions QCM basées sur ce document (minimum 20 questions):).
            Réponds UNIQUEMENT en JSON valide :
            {
              "questions": [
                {
                  "id": "q1",
                  "question": "La question posée ?",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correct": "Option A",
                  "explanation": "Explication détaillée de la bonne réponse"
                }
              ]
            }
        """.formatted(numberOfQuestions);

        return geminiService.generateAuto(
                        prompt, pdfBytes, textContent, "QUIZ", user)
                .map(json -> parseQuiz(json, course, user))
                .map(quizRepository::save);
    }

    // ── Surcharge sans PDF ─────────────────────────────────
    public Mono<Quiz> generateQuiz(Course course, User user,
                                   int numberOfQuestions) {
        return generateQuiz(course, user, numberOfQuestions,
                null, course.getContentText());
    }

    @SuppressWarnings("unchecked")
    private Quiz parseQuiz(String json, Course course, User user) {
        try {
            json = jsonRepairUtil.repair(json);
            Map<String, Object> data = objectMapper.readValue(json, Map.class);
            List<Map<String, Object>> questions =
                    (List<Map<String, Object>>) data.get("questions");

            if (questions == null || questions.isEmpty())
                throw new RuntimeException("Aucune question générée");

            return Quiz.builder()
                    .course(course)
                    .user(user)
                    .questions(questions)
                    .totalQuestions(questions.size())
                    .tokensUsed(json.length() / 4)
                    .build();

        } catch (Exception e) {
            log.error("Erreur parsing quiz : {}", e.getMessage());
            throw new RuntimeException("Erreur parsing quiz : "
                    + e.getMessage());
        }
    }
}