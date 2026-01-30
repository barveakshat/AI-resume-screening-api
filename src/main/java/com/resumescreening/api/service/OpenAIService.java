package com.resumescreening.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OpenAIService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.model}")
    private String model;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${app.url:http://localhost:8080}")
    private String appUrl;

    public OpenAIService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                         @Value("${openai.api.url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public String chatCompletion(String systemPrompt, String userPrompt) {
        try {
            // Log configuration
            log.info("=== OpenAI API Configuration ===");
            log.info("API URL: {}", apiUrl);
            log.info("Model: {}", model);
            log.info("API Key starts with: {}", apiKey != null ? apiKey.substring(0, Math.min(15, apiKey.length())) : "NULL");
            log.info("App URL (Referer): {}", appUrl);
            log.info("================================");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 2000);

            log.info("Request body: {}", objectMapper.writeValueAsString(requestBody));

            String response = webClient.post()
                    .uri("")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header("HTTP-Referer", appUrl)
                    .header("X-Title", "Resume Screening API")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            String content = root.at("/choices/0/message/content").asText();

            log.info("OpenAI response received, length: {}", content.length());
            return content;

        } catch (WebClientResponseException e) {
            log.error("=== WebClient Error Details ===");
            log.error("Status Code: {}", e.getStatusCode());
            log.error("Status Text: {}", e.getStatusText());
            log.error("Response Body: {}", e.getResponseBodyAsString());
            log.error("Headers: {}", e.getHeaders());
            log.error("================================");
            throw new RuntimeException("Failed to call OpenAI API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call OpenAI API", e);
        }
    }

    public String complete(String prompt) {
        return chatCompletion(
                "You are a helpful assistant that processes resumes and job descriptions.",
                prompt
        );
    }

    public String cleanJsonResponse(String response) {
        String cleaned = response.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }
}