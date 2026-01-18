package com.resumescreening.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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

    public OpenAIService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                         @Value("${openai.api.url}") String baseUrl) {
        // Use the base URL from config (OpenRouter URL)
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public String chatCompletion(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 2000);

            log.debug("Calling OpenAI API with model: {}", model);


            String response = webClient.post()
                    .uri("") // Empty since we're using the full base URL
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header("HTTP-Referer", "http://localhost:8080")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            String content = root.at("/choices/0/message/content").asText();

            log.info("OpenAI response received, length: {}", content.length());
            return content;

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