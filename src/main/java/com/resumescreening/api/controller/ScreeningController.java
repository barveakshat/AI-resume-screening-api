package com.resumescreening.api.controller;

import com.resumescreening.api.model.dto.request.BatchScreeningRequest;
import com.resumescreening.api.model.dto.request.ScreeningRequest;
import com.resumescreening.api.model.dto.response.ApiResponse;
import com.resumescreening.api.model.dto.response.JobPostingResponse;
import com.resumescreening.api.model.dto.response.ScreeningResultResponse;
import com.resumescreening.api.model.entity.Application;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.Recommendation;
import com.resumescreening.api.service.ApplicationService;
import com.resumescreening.api.service.JobPostingService;
import com.resumescreening.api.service.ScreeningService;
import com.resumescreening.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/screening")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')")
public class ScreeningController {

    private final ScreeningService screeningService;
    private final JobPostingService jobPostingService;
    private final ApplicationService applicationService;
    private final UserService userService;

    // ✅ Screen single application
    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<ScreeningResultResponse>> screenApplication(
            @Valid @RequestBody ScreeningRequest request,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        // Get application entity and validate ownership through job posting
        Application application = applicationService.getApplicationEntityById(request.getApplicationId());

        if (!application.getJobPosting().getUser().getId().equals(user.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You don't have permission to screen this application"));
        }

        // Check if already screened, if yes return existing result
        Optional<ScreeningResultResponse> existingResult =
                screeningService.getScreeningResultByApplicationId(application.getId());

        if (existingResult.isPresent()) {
            return ResponseEntity.ok(
                    ApiResponse.success("Application already screened - returning existing result",
                            existingResult.get())
            );
        }

        // Screen application
        ScreeningResultResponse result = screeningService.screenApplication(application);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application screened successfully", result));
    }

    // ✅ Batch screen all applications for a job
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<ScreeningResultResponse>>> batchScreenApplications(
            @Valid @RequestBody BatchScreeningRequest request,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        // Get job and validate ownership
        JobPostingResponse job = jobPostingService.getJobById(request.getJobPostingId());
        if (!job.getRecruiterId().equals(user.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You don't have permission to screen for this job"));
        }

        // Batch screen all applications for this job
        List<ScreeningResultResponse> results =
                screeningService.batchScreenApplications(request.getJobPostingId(), user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Batch screening completed", results));
    }

    // Get screening result by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScreeningResultResponse>> getScreeningResult(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        ScreeningResultResponse result = screeningService.getScreeningResult(id);

        // Validate ownership - need to check if user owns the job
        JobPostingResponse job = jobPostingService.getJobById(result.getJobPostingId());
        if (!job.getRecruiterId().equals(user.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You don't have permission to view this screening result"));
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // Get all screening results for a job
    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<List<ScreeningResultResponse>>> getJobScreeningResults(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        // Validate job ownership
        JobPostingResponse job = jobPostingService.getJobById(jobId);
        if (!job.getRecruiterId().equals(user.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You don't have permission to view screening results for this job"));
        }

        List<ScreeningResultResponse> results = screeningService.getScreeningResultsByJobId(jobId);

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ✅ Get top candidates (sorted by score)
    @GetMapping("/job/{jobId}/top-candidates")
    public ResponseEntity<ApiResponse<List<ScreeningResultResponse>>> getTopCandidates(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        // Validate job ownership
        JobPostingResponse job = jobPostingService.getJobById(jobId);
        if (!job.getRecruiterId().equals(user.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        List<ScreeningResultResponse> results = screeningService.getTopCandidates(jobId);

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ✅ Get candidates by recommendation level
    @GetMapping("/job/{jobId}/recommendation/{recommendation}")
    public ResponseEntity<ApiResponse<List<ScreeningResultResponse>>> getCandidatesByRecommendation(
            @PathVariable Long jobId,
            @PathVariable Recommendation recommendation,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        // Validate job ownership
        JobPostingResponse job = jobPostingService.getJobById(jobId);
        if (!job.getRecruiterId().equals(user.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        List<ScreeningResultResponse> results =
                screeningService.getCandidatesByRecommendation(jobId, recommendation);

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ✅ Get screening statistics for a job
    @GetMapping("/job/{jobId}/stats")
    public ResponseEntity<ApiResponse<ScreeningService.ScreeningStatistics>> getScreeningStats(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        // Validate job ownership
        JobPostingResponse job = jobPostingService.getJobById(jobId);
        if (!job.getRecruiterId().equals(user.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        ScreeningService.ScreeningStatistics stats = screeningService.getScreeningStatistics(jobId);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}