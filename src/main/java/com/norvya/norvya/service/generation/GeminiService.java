package com.norvya.norvya.service.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.norvya.norvya.config.GeminiProperties;
import com.norvya.norvya.entity.ApiLog;
import com.norvya.norvya.entity.User;
import com.norvya.norvya.repository.ApiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final WebClient        webClient;
    private final GeminiProperties geminiProperties;
    private final ApiLogRepository apiLogRepository;
    private final ObjectMapper     objectMapper;

    public Mono<String> generateAuto(String prompt,
                                     byte[] pdfBytes,
                                     String textContent,
                                     String generationType,
                                     User user) {
        if (pdfBytes != null && pdfBytes.length > 0) {
            log.info("Mode PDF natif — {} octets", pdfBytes.length);
            return generateFromPdf(pdfBytes, prompt, generationType, user);
        }
        log.info("Mode texte — {} caractères",
                textContent != null ? textContent.length() : 0);
        return generate(prompt, generationType, user);
    }

    public Mono<String> generate(String prompt,
                                 String generationType,
                                 User user) {
        long startTime = System.currentTimeMillis();

        log.info("Appel Gemini texte — modèle : {}",
                geminiProperties.getModel());

        Map<String, Object> requestBody = buildTextRequestBody(prompt);
        String url = buildUrl(":generateContent");

        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractText)
                .doOnSuccess(result -> {
                    log.info("✅ Gemini texte — {} caractères", result.length());
                    logApiCall(user, generationType,
                            countTokens(prompt), countTokens(result),
                            System.currentTimeMillis() - startTime, "SUCCESS");
                })
                .doOnError(error -> {
                    log.error("❌ Erreur Gemini texte : {}", error.getMessage());
                    logApiCall(user, generationType,
                            countTokens(prompt), 0,
                            System.currentTimeMillis() - startTime, "ERROR");
                })
                .retry(geminiProperties.getMaxRetries());
    }

    public Mono<String> generateFromPdf(byte[] pdfBytes,
                                        String prompt,
                                        String generationType,
                                        User user) {
        long startTime = System.currentTimeMillis();

        log.info("Appel Gemini PDF — modèle : {}, {} octets",
                geminiProperties.getModel(), pdfBytes.length);

        String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of(
                                        "inline_data", Map.of(
                                                "mime_type", "application/pdf",
                                                "data", base64Pdf
                                        )
                                ),
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature",      geminiProperties.getTemperature(),
                        "maxOutputTokens",  geminiProperties.getMaxOutputTokens(),
                        "responseMimeType", "application/json"
                )
        );

        String url = buildUrl(":generateContent");

        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::extractText)
                .doOnSuccess(result -> {
                    log.info("✅ Gemini PDF — {} caractères", result.length());
                    logApiCall(user, generationType,
                            pdfBytes.length / 4, countTokens(result),
                            System.currentTimeMillis() - startTime, "SUCCESS");
                })
                .doOnError(error -> {
                    log.error("❌ Erreur Gemini PDF : {}", error.getMessage());
                    logApiCall(user, generationType, 0, 0,
                            System.currentTimeMillis() - startTime, "ERROR");
                })
                .retry(geminiProperties.getMaxRetries());
    }

    public Flux<String> generateStream(String prompt,
                                       String generationType,
                                       User user) {
        long startTime = System.currentTimeMillis();

        Map<String, Object> requestBody = buildTextRequestBody(prompt);
        String url = buildUrl(":streamGenerateContent") + "&alt=sse";

        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .map(this::extractStreamChunk)
                .filter(chunk -> !chunk.isEmpty())
                .doOnComplete(() ->
                        logApiCall(user, generationType,
                                countTokens(prompt), 0,
                                System.currentTimeMillis() - startTime, "SUCCESS"))
                .doOnError(error -> {
                    log.error("❌ Erreur Gemini Stream : {}", error.getMessage());
                    logApiCall(user, generationType, 0, 0,
                            System.currentTimeMillis() - startTime, "ERROR");
                });
    }

    private Map<String, Object> buildTextRequestBody(String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", Map.of(
                        "temperature",      geminiProperties.getTemperature(),
                        "maxOutputTokens",  geminiProperties.getMaxOutputTokens(),
                        "responseMimeType", "application/json"
                )
        );
    }

    private String buildUrl(String suffix) {
        return geminiProperties.getBaseUrl()
                + "/" + geminiProperties.getModel()
                + suffix
                + "?key=" + geminiProperties.getKey();
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content =
                    (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>) content.get("parts");
            String text = (String) parts.get(0).get("text");

            text = text.trim();
            if (text.startsWith("```json")) text = text.substring(7);
            if (text.startsWith("```"))     text = text.substring(3);
            if (text.endsWith("```"))
                text = text.substring(0, text.length() - 3);

            return text.trim();

        } catch (Exception e) {
            log.error("Erreur extraction réponse Gemini : {}", e.getMessage());
            throw new RuntimeException("Impossible d'extraire la réponse Gemini");
        }
    }

    private String extractStreamChunk(String chunk) {
        try {
            if (chunk.startsWith("data: ")) {
                String json = chunk.substring(6);
                Map<String, Object> parsed =
                        objectMapper.readValue(json, Map.class);
                return extractText(parsed);
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private int countTokens(String text) {
        return text == null ? 0 : text.length() / 4;
    }

    private void logApiCall(User user, String type,
                            int tokensIn, int tokensOut,
                            long latencyMs, String status) {
        try {
            ApiLog apiLog = ApiLog.builder()
                    .user(user)
                    .generationType(type)
                    .tokensInput(tokensIn)
                    .tokensOutput(tokensOut)
                    .latencyMs((int) latencyMs)
                    .status(status)
                    .createdAt(LocalDateTime.now())
                    .build();
            apiLogRepository.save(apiLog);
        } catch (Exception e) {
            log.error("Erreur log API : {}", e.getMessage());
        }
    }
}