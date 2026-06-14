package com.norvya.norvya.service.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.norvya.norvya.entity.Course;
import com.norvya.norvya.entity.Lab;
import com.norvya.norvya.entity.User;
import com.norvya.norvya.repository.LabRepository;
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
public class LabService {

    private final GeminiService  geminiService;
    private final LabRepository  labRepository;
    private final PromptBuilder  promptBuilder;
    private final ObjectMapper   objectMapper;
    private final JsonRepairUtil jsonRepairUtil;

    // ── Génération avec PDF natif ──────────────────────────
    public Mono<Lab> generateLab(Course course,
                                 User user,
                                 byte[] pdfBytes,
                                 String textContent) {
        String prompt = """
            Crée un lab pratique guidé basé sur ce document.
            Conçois une mise en situation réelle avec des étapes claires.
            Réponds UNIQUEMENT en JSON valide :
            {
              "scenario_title": "Titre de la mise en situation",
              "objectives": ["Objectif 1", "Objectif 2"],
              "steps": [
                {
                  "order": 1,
                  "title": "Titre de l'étape",
                  "description": "Description détaillée",
                  "expected_output": "Résultat attendu"
                }
              ]
            }
        """;

        return geminiService.generateAuto(
                        prompt, pdfBytes, textContent, "LAB", user)
                .map(json -> parseLab(json, course, user))
                .map(labRepository::save);
    }

    // ── Surcharge sans PDF ─────────────────────────────────
    public Mono<Lab> generateLab(Course course, User user) {
        return generateLab(course, user, null, course.getContentText());
    }

    @SuppressWarnings("unchecked")
    private Lab parseLab(String json, Course course, User user) {
        try {
            json = jsonRepairUtil.repair(json);
            Map<String, Object> data = objectMapper.readValue(json, Map.class);

            List<Map<String, Object>> steps =
                    (List<Map<String, Object>>) data.get("steps");
            List<String> objectives =
                    (List<String>) data.get("objectives");

            if (steps == null || steps.isEmpty())
                throw new RuntimeException("Aucune étape générée");

            return Lab.builder()
                    .course(course)
                    .user(user)
                    .scenarioTitle((String) data.get("scenario_title"))
                    .steps(steps)
                    .objectives(objectives)
                    .tokensUsed(json.length() / 4)
                    .build();

        } catch (Exception e) {
            log.error("Erreur parsing lab : {}", e.getMessage());
            throw new RuntimeException("Erreur parsing lab : "
                    + e.getMessage());
        }
    }
}