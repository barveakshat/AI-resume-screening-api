package com.resumescreening.api.model.enums;

/**
 * AI recommendation levels after screening a resume.
 * Based on overall match score (0-100).
 */
public enum Recommendation {
    STRONG_FIT,     // Score >= 80 - Highly recommended
    GOOD_FIT,       // Score >= 60 - Recommended
    MODERATE_FIT,   // Score >= 40 - Consider with caution
    POOR_FIT        // Score < 40 - Not recommended
}