package com.resumescreening.api.controller;

import com.resumescreening.api.exception.UnauthorizedException;
import com.resumescreening.api.model.dto.request.BatchScreeningRequest;
import com.resumescreening.api.model.dto.request.ScreeningRequest;
import com.resumescreening.api.model.dto.response.ApiResponse;
import com.resumescreening.api.model.dto.response.BatchScreeningResponse;
import com.resumescreening.api.model.dto.response.JobPostingResponse;
import com.resumescreening.api.model.dto.response.ScreeningResultResponse;
import com.resumescreening.api.model.dto.response.ScreeningStatusResponse;
import com.resumescreening.api.model.entity.Application;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.Recommendation;
import com.resumescreening.api.service.ApplicationService;
import com.resumescreening.api.service.CurrentUserService;
import com.resumescreening.api.service.JobPostingService;
import com.resumescreening.api.service.ScreeningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/screening")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')")
public class ScreeningController {

    private final ScreeningService screeningService;
    private final JobPostingService jobPostingService;
    private final ApplicationService applicationService;
    private final CurrentUserService currentUserService;

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<ScreeningStatusResponse>> screenApplication(
            @Valid @RequestBody ScreeningRequest request,
            Authentication authentication
    ) {
        User user = currentUserService.getCurrentUser(authentication);
        Application application = applicationService.getApplicationEntityById(request.getApplicationId());

        if (!application.getJobPosting().getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You don't have permission to screen this application");
        }

        ScreeningStatusResponse result = screeningService.queueScreening(application);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(result.getMessage(), result));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<BatchScreeningResponse>> batchScreenApplications(
            @Valid @RequestBody BatchScreeningRequest request,
            Authentication authentication
    ) {
        User user = currentUserService.getCurrentUser(authentication);
        JobPostingResponse job = jobPostingService.getJobById(request.getJobPostingId());
        if (!job.getRecruiterId().equals(user.getId())) {
            throw new UnauthorizedException("You don't have permission to screen for this job");
        }

        BatchScreeningResponse result = screeningService.queueBatchScreening(request.getJobPostingId(), user);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Batch screening queued", result));
    }

    @GetMapping("/application/{applicationId}/status")
    public ResponseEntity<ApiResponse<ScreeningStatusResponse>> getApplicationScreeningStatus(
            @PathVariable Long applicationId,
            Authentication authentication
    ) {
        User user = currentUserService.getCurrentUser(authentication);
        ScreeningStatusResponse status = screeningService.getScreeningStatus(applicationId, user);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScreeningResultResponse>> getScreeningResult(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = currentUserService.getCurrentUser(authentication);
        ScreeningResultResponse result = screeningService.getScreeningResult(id);

        JobPostingResponse job = jobPostingService.getJobById(result.getJobPostingId());
        if (!job.getRecruiterId().equals(user.getId())) {
            throw new UnauthorizedException("You don't have permission to view this screening result");
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<List<ScreeningResultResponse>>> getJobScreeningResults(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        User user = currentUserService.getCurrentUser(authentication);
        validateJobOwnership(jobId, user, "You don't have permission to view screening results for this job");
        return ResponseEntity.ok(ApiResponse.success(screeningService.getScreeningResultsByJobId(jobId)));
    }

    @GetMapping("/job/{jobId}/top-candidates")
    public ResponseEntity<ApiResponse<List<ScreeningResultResponse>>> getTopCandidates(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        User user = currentUserService.getCurrentUser(authentication);
        validateJobOwnership(jobId, user, "Access denied");
        return ResponseEntity.ok(ApiResponse.success(screeningService.getTopCandidates(jobId)));
    }

    @GetMapping("/job/{jobId}/recommendation/{recommendation}")
    public ResponseEntity<ApiResponse<List<ScreeningResultResponse>>> getCandidatesByRecommendation(
            @PathVariable Long jobId,
            @PathVariable Recommendation recommendation,
            Authentication authentication
    ) {
        User user = currentUserService.getCurrentUser(authentication);
        validateJobOwnership(jobId, user, "Access denied");
        return ResponseEntity.ok(ApiResponse.success(
                screeningService.getCandidatesByRecommendation(jobId, recommendation)
        ));
    }

    @GetMapping("/job/{jobId}/stats")
    public ResponseEntity<ApiResponse<ScreeningService.ScreeningStatistics>> getScreeningStats(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        User user = currentUserService.getCurrentUser(authentication);
        validateJobOwnership(jobId, user, "Access denied");
        return ResponseEntity.ok(ApiResponse.success(screeningService.getScreeningStatistics(jobId)));
    }

    private void validateJobOwnership(Long jobId, User user, String message) {
        JobPostingResponse job = jobPostingService.getJobById(jobId);
        if (!job.getRecruiterId().equals(user.getId())) {
            throw new UnauthorizedException(message);
        }
    }
}
