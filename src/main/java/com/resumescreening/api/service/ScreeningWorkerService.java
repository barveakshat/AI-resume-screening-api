package com.resumescreening.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumescreening.api.exception.ResourceNotFoundException;
import com.resumescreening.api.model.dto.ParsedResumeData;
import com.resumescreening.api.model.dto.ScreeningAnalysis;
import com.resumescreening.api.model.entity.Application;
import com.resumescreening.api.model.entity.JobPosting;
import com.resumescreening.api.model.entity.Resume;
import com.resumescreening.api.model.entity.ScreeningResult;
import com.resumescreening.api.model.enums.ApplicationStatus;
import com.resumescreening.api.model.enums.Recommendation;
import com.resumescreening.api.model.enums.ScreeningStatus;
import com.resumescreening.api.repository.ApplicationRepository;
import com.resumescreening.api.repository.ScreeningResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreeningWorkerService {

    private final OpenAIService openAIService;
    private final ScreeningResultRepository screeningRepository;
    private final ApplicationRepository applicationRepository;
    private final ObjectMapper objectMapper;

    @Async("screeningTaskExecutor")
    @Caching(evict = {
            @CacheEvict(value = "screeningResults", allEntries = true),
            @CacheEvict(value = "jobScreeningResults", allEntries = true),
            @CacheEvict(value = "screeningStats", allEntries = true)
    })
    public CompletableFuture<Void> screenApplicationAsync(Long applicationId) {
        long startTime = System.currentTimeMillis();
        Application application = null;

        try {
            application = loadApplication(applicationId);
            markProcessing(application);

            if (screeningRepository.existsByApplicationId(applicationId)) {
                markCompleted(application, null);
                return CompletableFuture.completedFuture(null);
            }

            JobPosting job = application.getJobPosting();
            Resume resume = application.getResume();
            String prompt = buildScreeningPrompt(job, resume);
            String aiResponse = openAIService.complete(prompt);
            String cleanedResponse = openAIService.cleanJsonResponse(aiResponse);
            ScreeningAnalysis analysis = objectMapper.readValue(cleanedResponse, ScreeningAnalysis.class);
            long processingTime = System.currentTimeMillis() - startTime;

            ScreeningResult result = buildResult(application, analysis, processingTime);
            result = screeningRepository.save(result);
            Hibernate.initialize(result.getApplication());
            Hibernate.initialize(result.getJobPosting());

            markCompleted(application, result);
            log.info("Async screening completed for application {}", applicationId);
        } catch (Exception e) {
            log.warn("Async screening failed for application {}: {}", applicationId, e.getMessage());
            markFailed(applicationId, application, "Screening failed. Please retry later.");
        }

        return CompletableFuture.completedFuture(null);
    }

    private Application loadApplication(Long applicationId) {
        return applicationRepository.findByIdWithDetails(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));
    }

    private void markProcessing(Application application) {
        application.setScreeningStatus(ScreeningStatus.PROCESSING);
        application.setScreeningError(null);
        applicationRepository.save(application);
    }

    private void markCompleted(Application application, ScreeningResult result) {
        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        application.setScreeningStatus(ScreeningStatus.COMPLETED);
        application.setScreeningCompletedAt(LocalDateTime.now());
        application.setScreeningError(null);
        application.setScreenedAt(LocalDateTime.now());
        applicationRepository.save(application);
    }

    private void markFailed(Long applicationId, Application application, String message) {
        Application applicationToUpdate = application;
        if (applicationToUpdate == null) {
            applicationToUpdate = applicationRepository.findByIdWithDetails(applicationId).orElse(null);
        }
        if (applicationToUpdate == null) {
            return;
        }
        applicationToUpdate.setScreeningStatus(ScreeningStatus.FAILED);
        applicationToUpdate.setScreeningCompletedAt(LocalDateTime.now());
        applicationToUpdate.setScreeningError(message);
        applicationRepository.save(applicationToUpdate);
    }

    private ScreeningResult buildResult(Application application, ScreeningAnalysis analysis, long processingTime) {
        ScreeningResult result = new ScreeningResult();
        result.setApplication(application);
        result.setJobPosting(application.getJobPosting());
        result.setMatchScore(analysis.getOverallScore().intValue());
        result.setSkillMatchScore(analysis.getSkillMatchScore() != null ? analysis.getSkillMatchScore().intValue() : null);
        result.setExperienceMatchScore(analysis.getExperienceMatchScore() != null ? analysis.getExperienceMatchScore().intValue() : null);
        result.setEducationMatchScore(analysis.getEducationMatchScore() != null ? analysis.getEducationMatchScore().intValue() : null);
        result.setRecommendation(determineRecommendation(analysis.getOverallScore()));
        result.setMatchedSkills(analysis.getMatchedSkills());
        result.setMissingSkills(analysis.getMissingSkills());
        result.setStrengths(analysis.getStrengths());
        result.setWeaknesses(analysis.getWeaknesses());
        result.setAiAnalysis(analysis.getSummary());
        result.setProcessingTimeMs(processingTime);
        return result;
    }

    private String buildScreeningPrompt(JobPosting job, Resume resume) {
        ParsedResumeData parsedData = extractParsedData(resume);
        List<String> jobSkills = job.getRequiredSkills() != null ? job.getRequiredSkills() : List.of();
        List<String> candidateSkills = parsedData.getSkills() != null ? parsedData.getSkills() : List.of();

        return String.format("""
            You are an expert technical recruiter. Analyze how well this candidate matches the job requirements.

            JOB POSTING:
            Title: %s
            Required Skills: %s
            Experience Level: %s
            Description: %s

            CANDIDATE PROFILE:
            Name: %s
            Skills: %s
            Total Experience: %d years
            Education: %s

            Provide your analysis in the following JSON format (return ONLY JSON):
            {
                "overallScore": 0-100,
                "skillMatchScore": 0-100,
                "experienceMatchScore": 0-100,
                "educationMatchScore": 0-100,
                "matchedSkills": ["skill1", "skill2"],
                "missingSkills": ["skill3", "skill4"],
                "strengths": "Brief description of candidate strengths",
                "weaknesses": "Brief description of gaps or concerns",
                "summary": "2-3 sentence overall assessment",
                "keyHighlights": ["highlight1", "highlight2"]
            }
            """,
                job.getTitle(),
                String.join(", ", jobSkills),
                job.getExperienceLevel(),
                job.getDescription(),
                parsedData.getFullName(),
                String.join(", ", candidateSkills),
                parsedData.getTotalExperienceYears() != null ? parsedData.getTotalExperienceYears() : 0,
                formatEducation(parsedData)
        );
    }

    private ParsedResumeData extractParsedData(Resume resume) {
        try {
            if (resume.getParsedData() == null || resume.getParsedData().trim().isEmpty()) {
                throw new IllegalArgumentException("Resume has not been parsed yet");
            }
            return objectMapper.readValue(resume.getParsedData(), ParsedResumeData.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Resume parsed data is missing or invalid", e);
        }
    }

    private String formatEducation(ParsedResumeData data) {
        if (data.getEducation() == null || data.getEducation().isEmpty()) {
            return "Not specified";
        }

        ParsedResumeData.Education edu = data.getEducation().getFirst();
        return String.format("%s from %s (%s)",
                edu.getDegree() != null ? edu.getDegree() : "Unknown",
                edu.getInstitution() != null ? edu.getInstitution() : "Unknown",
                edu.getYear() != null ? edu.getYear() : "Unknown");
    }

    private Recommendation determineRecommendation(BigDecimal score) {
        if (score.compareTo(new BigDecimal("80")) >= 0) {
            return Recommendation.STRONG_FIT;
        } else if (score.compareTo(new BigDecimal("60")) >= 0) {
            return Recommendation.GOOD_FIT;
        } else if (score.compareTo(new BigDecimal("40")) >= 0) {
            return Recommendation.MODERATE_FIT;
        } else {
            return Recommendation.POOR_FIT;
        }
    }
}
