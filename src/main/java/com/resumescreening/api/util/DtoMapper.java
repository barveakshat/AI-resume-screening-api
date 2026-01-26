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

        return builder.build();
    }

    public static ResumeResponse toResumeResponse(Resume resume) {
        if (resume == null) {
            return null;
        }
        Long userId = null;
        if (resume.getUser() != null) {
            userId = resume.getUser().getId();
        }
        return ResumeResponse.builder()
                .id(resume.getId())
                .userId(userId)
                .fileName(resume.getFileName())
                .filePath(resume.getFilePath())
                .fileType(resume.getContentType())
                .fileSize(resume.getFileSize())
                .uploadDate(resume.getUploadedAt())
                .parsedData(null)
                .build();
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
        return ApplicationResponse.builder()
                .id(application.getId())
                .jobId(application.getJobPosting().getId())
                .jobTitle(application.getJobPosting().getTitle())
                .candidateId(application.getCandidate().getId())
                .candidateName(application.getCandidate().getFullName())
                .candidateEmail(application.getCandidate().getEmail())
                .resumeId(application.getResume().getId())
                .resumeTitle(application.getResume().getResumeTitle())
                .status(application.getStatus())
                .coverLetter(application.getCoverLetter())
                .appliedAt(application.getAppliedAt())
                .screenedAt(application.getScreenedAt())
                .build();
    }
}