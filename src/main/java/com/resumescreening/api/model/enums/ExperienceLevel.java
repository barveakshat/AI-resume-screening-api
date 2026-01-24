package com.resumescreening.api.model.enums;

import java.io.Serializable;

/**
 * Experience levels for job postings.
 * Helps match candidates based on their experience.
 */
public enum ExperienceLevel implements Serializable {
    ENTRY,      // 0-2 years
    MID,        // 2-5 years
    SENIOR,     // 5-10 years
    LEAD        // 10+ years
}