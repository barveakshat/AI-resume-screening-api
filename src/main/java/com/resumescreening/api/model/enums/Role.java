package com.resumescreening.api.model.enums;

/**
 * Defines user roles in the system.
 * - RECRUITER: Can create jobs, upload resumes, screen candidates
 * - CANDIDATE: Can upload their own resume, view parsed data
 */
public enum Role {
    RECRUITER,    // HR managers, hiring managers
    CANDIDATE     // Job seekers
}