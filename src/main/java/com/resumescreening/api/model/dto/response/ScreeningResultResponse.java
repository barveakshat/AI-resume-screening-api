package com.resumescreening.api.model.dto.response;

import com.resumescreening.api.model.enums.Recommendation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreeningResultResponse {
    private Long id;
    private Long applicationId;
    private Long jobPostingId;
    private String jobTitle;
    private Long resumeId;
    private String candidateName;
    private String candidateEmail;

    // Scores
    private Integer matchScore; // Overall score
    private Integer skillMatchScore;
    private Integer experienceMatchScore;
    private Integer educationMatchScore;

    private Recommendation recommendation;

    // Skills analysis
    private List<String> matchedSkills;
    private List<String> missingSkills;

    // Analysis
    private String strengths;
    private String weaknesses;
    private String aiAnalysis;

    // Metadata
    private Long processingTimeMs;
    private LocalDateTime screenedAt;
    private LocalDateTime createdAt;
}