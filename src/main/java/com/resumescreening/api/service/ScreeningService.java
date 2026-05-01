package com.resumescreening.api.service;

import com.resumescreening.api.exception.ResourceNotFoundException;
import com.resumescreening.api.exception.UnauthorizedException;
import com.resumescreening.api.model.dto.response.ApplicationResponse;
import com.resumescreening.api.model.dto.response.BatchScreeningResponse;
import com.resumescreening.api.model.dto.response.ScreeningResultResponse;
import com.resumescreening.api.model.dto.response.ScreeningStatusResponse;
import com.resumescreening.api.model.entity.Application;
import com.resumescreening.api.model.entity.ScreeningResult;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.Recommendation;
import com.resumescreening.api.model.enums.ScreeningStatus;
import com.resumescreening.api.repository.ApplicationRepository;
import com.resumescreening.api.repository.ScreeningResultRepository;
import com.resumescreening.api.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreeningService {
    private final ScreeningResultRepository screeningRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationService applicationService;
    private final ScreeningWorkerService screeningWorkerService;

    @Transactional
    public ScreeningStatusResponse queueScreening(Application application) {
        Optional<ScreeningResult> existingResult = screeningRepository.findByApplicationId(application.getId());
        if (existingResult.isPresent()) {
            application.setScreeningStatus(ScreeningStatus.COMPLETED);
            application.setScreeningCompletedAt(application.getScreeningCompletedAt() != null
                    ? application.getScreeningCompletedAt()
                    : LocalDateTime.now());
            application.setScreeningError(null);
            applicationRepository.save(application);
            return toStatusResponse(application, existingResult.get().getId(), "Application already screened");
        }

        if (application.getScreeningStatus() == ScreeningStatus.QUEUED
                || application.getScreeningStatus() == ScreeningStatus.PROCESSING) {
            return toStatusResponse(application, null, "Screening is already in progress");
        }

        application.setScreeningStatus(ScreeningStatus.QUEUED);
        application.setScreeningError(null);
        application.setScreeningRequestedAt(LocalDateTime.now());
        application.setScreeningCompletedAt(null);
        applicationRepository.save(application);

        screeningWorkerService.screenApplicationAsync(application.getId());
        return toStatusResponse(application, null, "Screening queued");
    }

    @Transactional
    public BatchScreeningResponse queueBatchScreening(Long jobId, User recruiter) {
        log.info("Queueing batch screening for job {}", jobId);
        List<ApplicationResponse> applications = applicationService.getApplicationsForJob(jobId, recruiter);
        List<ScreeningStatusResponse> statuses = new ArrayList<>();
        int queued = 0;
        int skipped = 0;
        int failed = 0;

        for (ApplicationResponse appResponse : applications) {
            try {
                Application application = applicationService.getApplicationEntityById(appResponse.getId());
                ScreeningStatus before = application.getScreeningStatus();
                ScreeningStatusResponse status = queueScreening(application);
                statuses.add(status);
                if (status.getScreeningStatus() == ScreeningStatus.QUEUED && before != ScreeningStatus.QUEUED) {
                    queued++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("Failed to queue screening for application {}: {}", appResponse.getId(), e.getMessage());
            }
        }

        return BatchScreeningResponse.builder()
                .jobPostingId(jobId)
                .totalApplications(applications.size())
                .queuedCount(queued)
                .skippedCount(skipped)
                .failedCount(failed)
                .applications(statuses)
                .build();
    }

    @Transactional(readOnly = true)
    public ScreeningStatusResponse getScreeningStatus(Long applicationId, User recruiter) {
        Application application = applicationService.getApplicationEntityById(applicationId);
        if (!application.getJobPosting().getUser().getId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only view screening status for your own jobs");
        }
        Long resultId = screeningRepository.findByApplicationId(applicationId)
                .map(ScreeningResult::getId)
                .orElse(null);
        return toStatusResponse(application, resultId, statusMessage(application.getScreeningStatus()));
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

        return new ScreeningStatistics(totalScreened, strongFit, goodFit, moderateFit, poorFit, averageScore);
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

    private ScreeningStatusResponse toStatusResponse(Application application, Long resultId, String message) {
        return ScreeningStatusResponse.builder()
                .applicationId(application.getId())
                .jobPostingId(application.getJobPosting().getId())
                .screeningResultId(resultId)
                .screeningStatus(application.getScreeningStatus())
                .message(message)
                .screeningError(application.getScreeningError())
                .screeningRequestedAt(application.getScreeningRequestedAt())
                .screeningCompletedAt(application.getScreeningCompletedAt())
                .build();
    }

    private String statusMessage(ScreeningStatus status) {
        return switch (status) {
            case NOT_STARTED -> "Screening has not started";
            case QUEUED -> "Screening queued";
            case PROCESSING -> "Screening is processing";
            case COMPLETED -> "Screening completed";
            case FAILED -> "Screening failed";
        };
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
