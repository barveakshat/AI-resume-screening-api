package com.resumescreening.api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningAnalysis {

    private BigDecimal overallScore;
    private BigDecimal skillMatchScore;
    private BigDecimal experienceMatchScore;
    private BigDecimal educationMatchScore;

    private List<String> matchedSkills = new ArrayList<>();
    private List<String> missingSkills = new ArrayList<>();

    private String strengths;
    private String weaknesses;
    private String summary;

    private List<String> keyHighlights = new ArrayList<>();
}