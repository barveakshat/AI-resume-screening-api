package com.resumescreening.api.util;

import com.resumescreening.api.model.dto.response.*;
import com.resumescreening.api.model.entity.*;
import com.resumescreening.api.model.dto.ParsedResumeData;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DtoMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // User to UserResponse
    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .companyName(user.getCompanyName())
                .designation(user.getDesignation())
                .phoneNumber(user.getPhoneNumber())
                .isEmailVerified(user.getIsEmailVerified())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // JobPosting to JobPostingResponse
    public static JobPostingResponse toJobPostingResponse(JobPosting job) {
        return JobPostingResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .requiredSkills(job.getRequiredSkills())
                .experienceLevel(job.getExperienceLevel())
                .employmentType(job.getEmploymentType())
                .location(job.getLocation())
                .salaryRange(job.getSalaryRange())
                .isActive(job.getIsActive())
                .createdAt(job.getCreatedAt())
                .build();
    }

    // Resume to ResumeResponse
    public static ResumeResponse toResumeResponse(Resume resume) {
        ParsedResumeData parsedData = null;
        if (resume.getParsedData() != null) {
            try {
                parsedData = objectMapper.convertValue(
                        resume.getParsedData(),
                        ParsedResumeData.class
                );
            } catch (Exception e) {
                // Handle conversion error
            }
        }

        return ResumeResponse.builder()
                .id(resume.getId())
                .fileName(resume.getFileName())
                .filePath(resume.getFilePath())
                .fileType(resume.getFileType())
                .fileSize(resume.getFileSize())
                .parsedData(parsedData)
                .uploadDate(resume.getUploadDate())
                .build();
    }

    // ScreeningResult to ScreeningResultResponse
    public static ScreeningResultResponse toScreeningResultResponse(ScreeningResult result) {
        return ScreeningResultResponse.builder()
                .id(result.getId())
                .jobPostingId(result.getJobPosting().getId())
                .resumeId(result.getResume().getId())
                .overallScore(result.getOverallScore())
                .skillMatchScore(result.getSkillMatchScore())
                .experienceMatchScore(result.getExperienceMatchScore())
                .educationMatchScore(result.getEducationMatchScore())
                .matchedSkills(convertJsonToList(result.getMatchedSkills()))
                .missingSkills(convertJsonToList(result.getMissingSkills()))
                .strengths(result.getStrengths())
                .weaknesses(result.getWeaknesses())
                .aiSummary(result.getAiSummary())
                .recommendation(result.getRecommendation())
                .processingTimeMs(result.getProcessingTimeMs())
                .screenedAt(result.getScreenedAt())
                .build();
    }

    // Helper to convert JsonNode to List<String>
    private static java.util.List<String> convertJsonToList(Object jsonNode) {
        try {
            if (jsonNode == null) return java.util.Collections.emptyList();
            return objectMapper.convertValue(
                    jsonNode,
                    objectMapper.getTypeFactory().constructCollectionType(
                            java.util.List.class,
                            String.class
                    )
            );
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }
}