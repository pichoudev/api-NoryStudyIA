package com.norvya.norvya.service.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.norvya.norvya.entity.Course;
import com.norvya.norvya.entity.Summary;
import com.norvya.norvya.entity.User;
import com.norvya.norvya.repository.SummaryRepository;
import com.norvya.norvya.util.JsonRepairUtil;
import com.norvya.norvya.util.PromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryService {

    private final GeminiService     geminiService;
    private final SummaryRepository summaryRepository;
    private final PromptBuilder     promptBuilder;
    private final ObjectMapper      objectMapper;
    private final JsonRepairUtil    jsonRepairUtil;

    private static final long MAX_PDF_SIZE = 5 * 1024 * 1024;

    // ── Génération avec PDF natif ou texte ─────────────────
    public Mono<Summary> generateSummary(Course course,
                                         User user,
                                         byte[] pdfBytes,
                                         String textContent) {

        boolean usePdf = pdfBytes != null
                && pdfBytes.length > 0
                && pdfBytes.length < MAX_PDF_SIZE;

        if (pdfBytes != null && !usePdf) {
            log.warn("PDF trop volumineux ({} Mo) — fallback texte",
                    pdfBytes.length / (1024 * 1024));
        }

        String prompt = """
            Analyse ce document en profondeur et génère un résumé COMPLET et DÉTAILLÉ.
            Inclus TOUT le contenu : texte, images, tableaux, formules, graphiques.
            Le résumé doit permettre à un étudiant de réviser SANS relire le cours.

            RÈGLES IMPORTANTES :
            - Réponds UNIQUEMENT en JSON valide
            - Pas de texte avant ou après le JSON
            - Pas de commentaires dans le JSON
            - Toutes les valeurs de type texte doivent être des strings
            - Les tableaux ne doivent pas avoir de virgules finales

            Format EXACT à respecter :
            {
              "title": "Titre du cours",
              "overview": "Vue d'ensemble en 3-4 phrases",
              "sections": [
                {
                  "heading": "Titre de la section",
                  "body": "Contenu détaillé",
                  "key_concepts": ["concept1", "concept2"],
                  "formulas": ["formule si applicable"],
                  "example": "exemple concret"
                }
              ],
              "key_points": ["point clé 1", "point clé 2"],
              "glossary": [
                {"term": "terme", "definition": "définition"}
              ],
              "important_notes": ["note 1", "note 2"],
              "summary": "Résumé final en 3-4 phrases"
            }
        """;

        return geminiService.generateAuto(
                        prompt,
                        usePdf ? pdfBytes : null,
                        textContent,
                        "SUMMARY",
                        user)
                .map(json -> parseSummary(json, course, user))
                .map(summaryRepository::save);
    }

    // ── Surcharge sans PDF ─────────────────────────────────
    public Mono<Summary> generateSummary(Course course, User user) {
        String prompt = promptBuilder.buildSummaryPrompt(
                course.getContentText()
        );
        return geminiService.generate(prompt, "SUMMARY", user)
                .map(json -> parseSummary(json, course, user))
                .map(summaryRepository::save);
    }

    // ── Streaming ──────────────────────────────────────────
    public Flux<String> generateSummaryStream(Course course, User user) {
        String prompt = promptBuilder.buildSummaryPrompt(
                course.getContentText()
        );
        return geminiService.generateStream(prompt, "SUMMARY", user);
    }

    // ── Parser ─────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Summary parseSummary(String json, Course course, User user) {
        try {
            // ✅ Logger le JSON brut AVANT réparation
            log.info("=== JSON BRUT GEMINI ===\n{}", json);

            json = jsonRepairUtil.repair(json);

            // ✅ Logger APRÈS réparation
            log.info("=== JSON RÉPARÉ ===\n{}", json);

            Map<String, Object> data = objectMapper.readValue(json, Map.class);

            List<Map<String, Object>> sections =
                    data.get("sections") != null
                            ? (List<Map<String, Object>>) data.get("sections")
                            : new ArrayList<>();

            List<String> keyPoints =
                    data.get("key_points") != null
                            ? (List<String>) data.get("key_points")
                            : new ArrayList<>();

            List<Map<String, Object>> glossary =
                    data.get("glossary") != null
                            ? (List<Map<String, Object>>) data.get("glossary")
                            : new ArrayList<>();

            return Summary.builder()
                    .course(course)
                    .user(user)
                    .sections(sections)
                    .keyPoints(keyPoints)
                    .glossary(glossary)
                    .tokensUsed(json.length() / 4)
                    .build();

        } catch (Exception e) {
            log.error("Erreur parsing résumé : {}", e.getMessage());
            log.error("=== JSON PROBLÉMATIQUE ===\n{}", json);
            throw new RuntimeException("Erreur parsing résumé : " + e.getMessage());
        }
    }
}