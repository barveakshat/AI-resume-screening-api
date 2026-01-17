package com.resumescreening.api.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreeningAnalysis {

    @JsonProperty("overallScore")
    private BigDecimal overallScore; // 0-100

    @JsonProperty("skillMatchScore")
    private BigDecimal skillMatchScore; // 0-100

    @JsonProperty("experienceMatchScore")
    private BigDecimal experienceMatchScore; // 0-100

    @JsonProperty("educationMatchScore")
    private BigDecimal educationMatchScore; // 0-100

    @JsonProperty("matchedSkills")
    private List<String> matchedSkills;

    @JsonProperty("missingSkills")
    private List<String> missingSkills;

    @JsonProperty("strengths")
    private String strengths;

    @JsonProperty("weaknesses")
    private String weaknesses;

    @JsonProperty("summary")
    private String summary; // AI analysis summary

    @JsonProperty("keyHighlights")
    private List<String> keyHighlights;
}