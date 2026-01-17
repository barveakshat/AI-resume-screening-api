package com.resumescreening.api.model.enums;

public enum ApplicationStatus {
    PENDING,        // Application submitted, not yet screened
    UNDER_REVIEW,   // Being reviewed by recruiter
    SHORTLISTED,    // Passed screening
    REJECTED,       // Did not pass screening
    INTERVIEWED,    // Candidate interviewed
    HIRED,          // Candidate hired
    WITHDRAWN       // Candidate withdrew application
}