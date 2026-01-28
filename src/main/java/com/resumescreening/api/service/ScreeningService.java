package com.resumescreening.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumescreening.api.exception.ResourceNotFoundException;
import com.resumescreening.api.model.dto.ParsedResumeData;
import com.resumescreening.api.model.dto.ScreeningAnalysis;
import com.resumescreening.api.model.dto.response.ApplicationResponse;
import com.resumescreening.api.model.dto.response.ScreeningResultResponse;
import com.resumescreening.api.model.entity.Application;
import com.resumescreening.api.model.entity.JobPosting;
import com.resumescreening.api.model.entity.Resume;
import com.resumescreening.api.model.entity.ScreeningResult;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.ApplicationStatus;
import com.resumescreening.api.model.enums.Recommendation;
import com.resumescreening.api.repository.ScreeningResultRepository;
import com.resumescreening.api.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreeningService {
    private final OpenAIService openAIService;
    private final ScreeningResultRepository screeningRepository;
    private final ApplicationService applicationService;
    private final ObjectMapper objectMapper;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "screeningResults", key = "#application.id"),
            @CacheEvict(value = "jobScreeningResults", key = "#application.jobPosting.id"),
            @CacheEvict(value = "screeningStats", key = "#application.jobPosting.id")
    })
    public ScreeningResultResponse screenApplication(Application application) {
        long startTime = System.currentTimeMillis();

        try {
            // Check if already screened
            if (screeningRepository.existsByApplicationId(application.getId())) {
                throw new IllegalStateException("This application has already been screened");
            }
            log.info("Screening application {} for job {}",
                    application.getId(),
                    application.getJobPosting().getId());

            JobPosting job = application.getJobPosting();
            Resume resume = application.getResume();
            String prompt = buildScreeningPrompt(job, resume);
            String aiResponse = openAIService.complete(prompt);
            String cleanedResponse = openAIService.cleanJsonResponse(aiResponse);
            ScreeningAnalysis analysis = objectMapper.readValue(
                    cleanedResponse,
                    ScreeningAnalysis.class
            );
            long processingTime = System.currentTimeMillis() - startTime;

            ScreeningResult result = new ScreeningResult();
            result.setApplication(application);
            result.setJobPosting(application.getJobPosting());
            result.setMatchScore(analysis.getOverallScore().intValue());
            result.setSkillMatchScore(analysis.getSkillMatchScore() != null ?
                    analysis.getSkillMatchScore().intValue() : null);
            result.setExperienceMatchScore(analysis.getExperienceMatchScore() != null ?
                    analysis.getExperienceMatchScore().intValue() : null);
            result.setEducationMatchScore(analysis.getEducationMatchScore() != null ?
                    analysis.getEducationMatchScore().intValue() : null);
            result.setRecommendation(determineRecommendation(analysis.getOverallScore()));
            result.setMatchedSkills(analysis.getMatchedSkills());
            result.setMissingSkills(analysis.getMissingSkills());
            result.setStrengths(analysis.getStrengths());
            result.setWeaknesses(analysis.getWeaknesses());
            result.setAiAnalysis(analysis.getSummary());
            result.setProcessingTimeMs(processingTime);
            result = screeningRepository.save(result);

            Hibernate.initialize(result.getApplication());
            Hibernate.initialize(result.getJobPosting());

            application.setStatus(ApplicationStatus.UNDER_REVIEW);
            application.setScreenedAt(LocalDateTime.now());

            log.info("Screening completed: Score={}, Recommendation={}, Time={}ms",
                    result.getMatchScore(), result.getRecommendation(), processingTime);
            return DtoMapper.toScreeningResultResponse(result);
        } catch (Exception e) {
            log.error("Error screening application: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to screen application", e);
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "jobScreeningResults", key = "#jobId"),
            @CacheEvict(value = "screeningStats", key = "#jobId")
    })
    public List<ScreeningResultResponse> batchScreenApplications(Long jobId, User recruiter) {
        log.info("Batch screening applications for job {}", jobId);
        List<ApplicationResponse> applications = applicationService.getApplicationsForJob(jobId, recruiter);
        List<ScreeningResultResponse> results = new ArrayList<>();

        for (ApplicationResponse appResponse : applications) {
            try {
                // Skip if already screened
                if (!screeningRepository.existsByApplicationId(appResponse.getId())) {
                    // Get the application entity
                    Application application = applicationService.getApplicationEntityById(appResponse.getId());
                    ScreeningResultResponse result = screenApplication(application);
                    results.add(result);
                } else {
                    log.info("Skipping already screened application: {}", appResponse.getId());
                }
            } catch (Exception e) {
                log.error("Error screening application {}: {}", appResponse.getId(), e.getMessage());
            }
        }
        log.info("Batch screening completed: {} results", results.size());
        return results;
    }

    @Cacheable(value = "screeningResults", key = "#screeningId")
    @Transactional(readOnly = true)
    public ScreeningResultResponse getScreeningResult(Long screeningId) {
        ScreeningResult result = getScreeningResultEntityById(screeningId);
        return DtoMapper.toScreeningResultResponse(result);
    }

    @Transactional(readOnly = true)
    public ScreeningResult getScreeningResultEntityById(Long screeningId) {
        ScreeningResult result = screeningRepository.findById(screeningId)
                .orElseThrow(() -> new ResourceNotFoundException("Screening result not found: " + screeningId));
        // Initialize relationships
        Hibernate.initialize(result.getApplication());
        Hibernate.initialize(result.getJobPosting());
        return result;
    }

    @Cacheable(value = "jobScreeningResults", key = "#jobId")
    @Transactional(readOnly = true)
    public List<ScreeningResultResponse> getScreeningResultsByJobId(Long jobId) {
        List<ScreeningResult> results = screeningRepository.findByApplicationJobPostingId(jobId);
        return results.stream()
                .map(DtoMapper::toScreeningResultResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScreeningResultResponse> getTopCandidates(Long jobId) {
        // Reuses cached job screening results
        List<ScreeningResultResponse> results = getScreeningResultsByJobId(jobId);
        return results.stream()
                .sorted((r1, r2) -> Integer.compare(r2.getMatchScore(), r1.getMatchScore()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScreeningResultResponse> getCandidatesByRecommendation(Long jobId, Recommendation recommendation) {
        List<ScreeningResult> results = screeningRepository.findByJobPostingIdAndRecommendation(jobId, recommendation);
        return results.stream()
                .map(DtoMapper::toScreeningResultResponse)
                .toList();
    }

    public boolean applicationAlreadyScreened(Long applicationId) {
        return screeningRepository.existsByApplicationId(applicationId);
    }

    @Cacheable(value = "screeningStats", key = "#jobId")
    @Transactional(readOnly = true)
    public ScreeningStatistics getScreeningStatistics(Long jobId) {
        List<ScreeningResult> results = screeningRepository.findByApplicationJobPostingId(jobId);

        long totalScreened = results.size();
        long strongFit = results.stream()
                .filter(r -> r.getRecommendation() == Recommendation.STRONG_FIT)
                .count();
        long goodFit = results.stream()
                .filter(r -> r.getRecommendation() == Recommendation.GOOD_FIT)
                .count();
        long moderateFit = results.stream()
                .filter(r -> r.getRecommendation() == Recommendation.MODERATE_FIT)
                .count();
        long poorFit = results.stream()
                .filter(r -> r.getRecommendation() == Recommendation.POOR_FIT)
                .count();

        double averageScore = results.stream()
                .mapToInt(ScreeningResult::getMatchScore)
                .average()
                .orElse(0.0);

        return new ScreeningStatistics(
                totalScreened,
                strongFit,
                goodFit,
                moderateFit,
                poorFit,
                averageScore
        );
    }

    public double getAverageScoreForJob(Long jobId) {
        List<ScreeningResult> results = screeningRepository.findByApplicationJobPostingId(jobId);
        return results.stream()
                .mapToInt(ScreeningResult::getMatchScore)
                .average()
                .orElse(0.0);
    }

    public long countByRecommendation(Long jobId, Recommendation recommendation) {
        return screeningRepository.findByJobPostingIdAndRecommendation(jobId, recommendation).size();
    }


    @Transactional(readOnly = true)
    public Optional<ScreeningResultResponse> getScreeningResultByApplicationId(Long applicationId) {
        Optional<ScreeningResult> result = screeningRepository.findByApplicationId(applicationId);
        return result.map(DtoMapper::toScreeningResultResponse);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private String buildScreeningPrompt(JobPosting job, Resume resume) {
        ParsedResumeData parsedData = extractParsedData(resume);

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
            
            Scoring guidelines:
            - overallScore: Weighted average (skills: 40%%, experience: 35%%, education: 25%%)
            - skillMatchScore: Percentage of required skills the candidate has
            - experienceMatchScore: How well experience level matches (fresher for entry-level can be 70-80)
            - educationMatchScore: Relevance and quality of education
            
            Be objective and specific. Consider projects as valid experience for freshers.
            """,
                job.getTitle(),
                String.join(", ", job.getRequiredSkills()),
                job.getExperienceLevel(),
                job.getDescription(),
                parsedData.getFullName(),
                String.join(", ", parsedData.getSkills()),
                parsedData.getTotalExperienceYears() != null ? parsedData.getTotalExperienceYears() : 0,
                formatEducation(parsedData)
        );
    }

    private ParsedResumeData extractParsedData(Resume resume) {
        try {
            if (resume.getParsedData() == null) {
                return new ParsedResumeData();
            }
            return objectMapper.convertValue(resume.getParsedData(), ParsedResumeData.class);
        } catch (Exception e) {
            log.warn("Could not extract parsed data from resume", e);
            return new ParsedResumeData();
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

    public record ScreeningStatistics(
            long totalScreened,
            long strongFit,
            long goodFit,
            long moderateFit,
            long poorFit,
            double averageScore
    ) {}
}