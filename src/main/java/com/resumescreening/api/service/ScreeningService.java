package com.resumescreening.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumescreening.api.exception.ResourceNotFoundException;
import com.resumescreening.api.model.dto.ParsedResumeData;
import com.resumescreening.api.model.dto.ScreeningAnalysis;
import com.resumescreening.api.model.entity.JobPosting;
import com.resumescreening.api.model.entity.Resume;
import com.resumescreening.api.model.entity.ScreeningResult;
import com.resumescreening.api.model.enums.Recommendation;
import com.resumescreening.api.repository.ScreeningResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreeningService {

    private final OpenAIService openAIService;
    private final ScreeningResultRepository screeningRepository;
    private final ObjectMapper objectMapper;

    // Screen resume against job posting
    @Transactional
    public ScreeningResult screenResume(JobPosting job, Resume resume) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Screening resume {} for job {}", resume.getId(), job.getId());

            // Build screening prompt
            String prompt = buildScreeningPrompt(job, resume);

            // Call OpenAI
            String aiResponse = openAIService.complete(prompt);

            // Parse response
            String cleanedResponse = openAIService.cleanJsonResponse(aiResponse);
            ScreeningAnalysis analysis = objectMapper.readValue(
                    cleanedResponse,
                    ScreeningAnalysis.class
            );

            // Create screening result
            ScreeningResult result = new ScreeningResult();
            result.setJobPosting(job);
            result.setResume(resume);
            result.setOverallScore(analysis.getOverallScore());
            result.setSkillMatchScore(analysis.getSkillMatchScore());
            result.setExperienceMatchScore(analysis.getExperienceMatchScore());
            result.setEducationMatchScore(analysis.getEducationMatchScore());
            result.setMatchedSkills(objectMapper.valueToTree(analysis.getMatchedSkills()));
            result.setMissingSkills(objectMapper.valueToTree(analysis.getMissingSkills()));
            result.setStrengths(analysis.getStrengths());
            result.setWeaknesses(analysis.getWeaknesses());
            result.setAiSummary(analysis.getSummary());
            result.setRecommendation(determineRecommendation(analysis.getOverallScore()));
            result.setProcessingTimeMs((int)(System.currentTimeMillis() - startTime));
            result.setScreenedAt(LocalDateTime.now());

            // Save to database
            result = screeningRepository.save(result);

            log.info("Screening completed: Score={}, Recommendation={}",
                    result.getOverallScore(), result.getRecommendation());

            return result;

        } catch (Exception e) {
            log.error("Error screening resume: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to screen resume", e);
        }
    }

    // Get screening result by ID
    public ScreeningResult getScreeningResult(Long screeningId) {
        return screeningRepository.findById(screeningId)
                .orElseThrow(() -> new ResourceNotFoundException("Screening result not found: " + screeningId));
    }

    // Get all screening results for a job
    public List<ScreeningResult> getScreeningResultsForJob(Long jobId) {
        return screeningRepository.findByJobPostingId(jobId);
    }

    // Get screening results sorted by score (top candidates)
    public List<ScreeningResult> getTopCandidates(Long jobId) {
        return screeningRepository.findByJobPostingIdOrderByOverallScoreDesc(jobId);
    }

    // Get screening results by recommendation
    public List<ScreeningResult> getCandidatesByRecommendation(Long jobId, Recommendation recommendation) {
        return screeningRepository.findByJobPostingIdAndRecommendation(jobId, recommendation);
    }

    // Check if screening exists
    public boolean screeningExists(Long jobId, Long resumeId) {
        return screeningRepository.existsByJobPostingIdAndResumeId(jobId, resumeId);
    }

    // Batch screen multiple resumes
    @Transactional
    public List<ScreeningResult> batchScreenResumes(JobPosting job, List<Resume> resumes) {
        log.info("Batch screening {} resumes for job {}", resumes.size(), job.getId());

        List<ScreeningResult> results = new ArrayList<>();

        for (Resume resume : resumes) {
            try {
                // Skip if already screened
                if (!screeningExists(job.getId(), resume.getId())) {
                    ScreeningResult result = screenResume(job, resume);
                    results.add(result);
                } else {
                    log.info("Skipping already screened resume: {}", resume.getId());
                }
            } catch (Exception e) {
                log.error("Error screening resume {}: {}", resume.getId(), e.getMessage());
            }
        }

        log.info("Batch screening completed: {} results", results.size());
        return results;
    }

    // Get average score for a job
    public BigDecimal getAverageScoreForJob(Long jobId) {
        BigDecimal avgScore = screeningRepository.getAverageScoreForJob(jobId);
        return avgScore != null ? avgScore : BigDecimal.ZERO;
    }

    // Count screening results by recommendation
    public long countByRecommendation(Long jobId, Recommendation recommendation) {
        return screeningRepository.countByJobPostingIdAndRecommendation(jobId, recommendation);
    }

    // Build screening prompt
    private String buildScreeningPrompt(JobPosting job, Resume resume) {
        // Get parsed resume data
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

    // Extract parsed data from resume
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

    // Format education for prompt
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

    // Determine recommendation based on score
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