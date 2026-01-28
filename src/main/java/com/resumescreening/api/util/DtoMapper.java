package com.resumescreening.api.util;

import com.resumescreening.api.model.dto.response.*;
import com.resumescreening.api.model.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.Hibernate;

public class DtoMapper {

    static {
        new ObjectMapper();
    }

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

    public static JobPostingResponse toJobPostingResponse(JobPosting job) {
        if (job == null) {
            return null;
        }

        JobPostingResponse.JobPostingResponseBuilder builder = JobPostingResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .requiredSkills(job.getRequiredSkills())
                .experienceLevel(job.getExperienceLevel())
                .employmentType(job.getEmploymentType())
                .location(job.getLocation())
                .salaryRange(job.getSalaryRange())
                .companyName(job.getCompanyName())
                .isActive(job.getIsActive())
                .createdAt(job.getCreatedAt());

        // Extract user fields individually - THIS IS THE KEY PART
        if (Hibernate.isInitialized(job.getUser()) && job.getUser() != null) {
            builder.recruiterId(job.getUser().getId())
                    .recruiterName(job.getUser().getFullName())
                    .recruiterEmail(job.getUser().getEmail());
        }

        return builder.build();
    }

    // ScreeningResult to ScreeningResultResponse
    public static ScreeningResultResponse toScreeningResultResponse(ScreeningResult result) {
        if (result == null) {
            return null;
        }

        Application application = result.getApplication();
        Resume resume = application.getResume();
        User candidate = application.getCandidate();

        return ScreeningResultResponse.builder()
                .id(result.getId())
                .applicationId(application.getId())
                .jobPostingId(application.getJobPosting().getId())
                .jobTitle(application.getJobPosting().getTitle())
                .resumeId(resume.getId())
                .candidateName(candidate.getFullName())
                .candidateEmail(candidate.getEmail())
                .matchScore(result.getMatchScore())
                .skillMatchScore(result.getSkillMatchScore())
                .experienceMatchScore(result.getExperienceMatchScore())
                .educationMatchScore(result.getEducationMatchScore())
                .recommendation(result.getRecommendation())
                .matchedSkills(result.getMatchedSkills())
                .missingSkills(result.getMissingSkills())
                .strengths(result.getStrengths())
                .weaknesses(result.getWeaknesses())
                .aiAnalysis(result.getAiAnalysis())
                .processingTimeMs(result.getProcessingTimeMs())
                .screenedAt(application.getScreenedAt())
                .createdAt(result.getCreatedAt())
                .build();
    }

    public static ApplicationResponse toApplicationResponse(Application application) {
        if (application == null) {
            return null;
        }
        ApplicationResponse.ApplicationResponseBuilder builder = ApplicationResponse.builder()
                .id(application.getId())
                .status(application.getStatus())
                .coverLetter(application.getCoverLetter())
                .appliedAt(application.getAppliedAt())
                .screenedAt(application.getScreenedAt());

        // Safely access jobPosting
        if (Hibernate.isInitialized(application.getJobPosting()) && application.getJobPosting() != null) {
            builder.jobId(application.getJobPosting().getId())
                    .jobTitle(application.getJobPosting().getTitle());
        }

        // Safely access candidate
        if (Hibernate.isInitialized(application.getCandidate()) && application.getCandidate() != null) {
            builder.candidateId(application.getCandidate().getId())
                    .candidateName(application.getCandidate().getFullName())
                    .candidateEmail(application.getCandidate().getEmail());
        }

        // Safely access resume
        if (Hibernate.isInitialized(application.getResume()) && application.getResume() != null) {
            builder.resumeId(application.getResume().getId())
                    .resumeTitle(application.getResume().getFileName());
        }
        return builder.build();
    }
}