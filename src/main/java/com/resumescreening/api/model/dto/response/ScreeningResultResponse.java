package com.resumescreening.api.model.dto.response;

import com.resumescreening.api.model.enums.Recommendation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningResultResponse {

    private Long id;
    private Long jobPostingId;
    private Long resumeId;

    // Scores
    private BigDecimal overallScore;
    private BigDecimal skillMatchScore;
    private BigDecimal experienceMatchScore;
    private BigDecimal educationMatchScore;

    // Analysis
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private String strengths;
    private String weaknesses;
    private String aiSummary;
    private Recommendation recommendation;

    // Metadata
    private Integer processingTimeMs;
    private LocalDateTime screenedAt;

    // Optional: Include candidate info
    private String candidateName;
    private String candidateEmail;
}