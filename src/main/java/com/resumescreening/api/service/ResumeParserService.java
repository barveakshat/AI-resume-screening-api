package com.resumescreening.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumescreening.api.model.dto.ParsedResumeData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeParserService {

    private final OpenAIService openAIService;
    private final ObjectMapper objectMapper;

    public ParsedResumeData parseResume(String resumeText) {
        try {
            String prompt = buildParsingPrompt(resumeText);

            log.info("Parsing resume with AI...");

            String aiResponse = openAIService.complete(prompt);

            String cleanedResponse = openAIService.cleanJsonResponse(aiResponse);

            ParsedResumeData parsedData = objectMapper.readValue(
                    cleanedResponse,
                    ParsedResumeData.class
            );

            log.info("Resume parsed successfully: {}", parsedData.getFullName());
            return parsedData;

        } catch (Exception e) {
            log.error("Error parsing resume: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse resume", e);
        }
    }

    private String buildParsingPrompt(String resumeText) {
        return String.format("""
            You are an expert resume parser. Extract the following information from this resume and return ONLY valid JSON.
            
            Required JSON structure:
            {
                "fullName": "string",
                "email": "string",
                "phone": "string",
                "skills": ["skill1", "skill2"],
                "totalExperienceYears": number,
                "experience": [
                    {
                        "title": "string",
                        "company": "string",
                        "duration": "string",
                        "description": "string"
                    }
                ],
                "education": [
                    {
                        "degree": "string",
                        "institution": "string",
                        "year": "string",
                        "field": "string"
                    }
                ],
                "summary": "brief professional summary"
            }
            
            Instructions:
            1. Extract ALL skills mentioned (technical and soft skills)
            2. For totalExperienceYears: If fresher/student, use 0
            3. For experience: Include projects if no work experience
            4. If information is missing, use empty string "" or empty array []
            5. Return ONLY the JSON, no explanations
            
            Resume text:
            %s
            """, resumeText);
    }
}