package com.norvya.norvya.util;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    // ── RÉSUMÉ ─────────────────────────────────────────────
    public String buildSummaryPrompt(String courseText) {
        return """
            Tu es un expert pédagogique. Analyse ce cours et génère un résumé structuré.
            Réponds UNIQUEMENT en JSON valide, sans texte avant ou après.

            Format de réponse :
            {
              "sections": [
                { "heading": "Titre de la section", "body": "Contenu détaillé..." }
              ],
              "key_points": ["Point clé 1", "Point clé 2", "Point clé 3"],
              "glossary": [
                { "term": "Terme", "definition": "Définition claire" }
              ]
            }

            Cours à analyser :
            %s
        """.formatted(courseText);
    }

    // ── QUIZ ───────────────────────────────────────────────
    public String buildQuizPrompt(String courseText, int numberOfQuestions) {
        return """
            Tu es un expert en création de QCM pédagogiques.
            Génère %d questions à choix multiples basées sur ce cours.
            Réponds UNIQUEMENT en JSON valide, sans texte avant ou après.

            Format de réponse :
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

            Cours :
            %s
        """.formatted(numberOfQuestions, courseText);
    }

    // ── FLASHCARDS ─────────────────────────────────────────
    public String buildFlashcardPrompt(String courseText, int numberOfCards) {
        return """
            Tu es un expert en mémorisation. Génère %d flashcards
            basées sur les concepts clés de ce cours.
            Réponds UNIQUEMENT en JSON valide, sans texte avant ou après.

            Format de réponse :
            {
              "cards": [
                {
                  "id": "f1",
                  "front": "Question ou terme",
                  "back": "Réponse ou définition concise"
                }
              ]
            }

            Cours :
            %s
        """.formatted(numberOfCards, courseText);
    }

    // ── EXERCICES ──────────────────────────────────────────
    public String buildExercisePrompt(String courseText, int numberOfProblems) {
        return """
            Tu es un professeur expert. Génère %d exercices pratiques
            basés sur ce cours, avec différents niveaux de difficulté.
            Réponds UNIQUEMENT en JSON valide, sans texte avant ou après.

            Format de réponse :
            {
              "problems": [
                {
                  "id": "p1",
                  "statement": "Énoncé de l'exercice",
                  "difficulty": "FACILE",
                  "hints": ["Indice 1", "Indice 2"],
                  "solution": "Solution détaillée étape par étape"
                }
              ]
            }

            Niveaux de difficulté : FACILE, MOYEN, DIFFICILE

            Cours :
            %s
        """.formatted(numberOfProblems, courseText);
    }

    // ── LAB ────────────────────────────────────────────────
    public String buildLabPrompt(String courseText) {
        return """
            Tu es un formateur expert. Crée un lab pratique guidé
            basé sur ce cours, avec une mise en situation réelle.
            Réponds UNIQUEMENT en JSON valide, sans texte avant ou après.

            Format de réponse :
            {
              "scenario_title": "Titre de la mise en situation",
              "objectives": ["Objectif 1", "Objectif 2", "Objectif 3"],
              "steps": [
                {
                  "order": 1,
                  "title": "Titre de l'étape",
                  "description": "Description détaillée de ce qu'il faut faire",
                  "expected_output": "Résultat attendu à cette étape"
                }
              ]
            }

            Cours :
            %s
        """.formatted(courseText);
    }
}