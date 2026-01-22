package com.resumescreening.api.controller;

import com.resumescreening.api.model.dto.request.BatchScreeningRequest;
import com.resumescreening.api.model.dto.request.ScreeningRequest;
import com.resumescreening.api.model.dto.response.ApiResponse;
import com.resumescreening.api.model.dto.response.ScreeningResultResponse;
import com.resumescreening.api.model.entity.Application;
import com.resumescreening.api.model.entity.JobPosting;
import com.resumescreening.api.model.entity.ScreeningResult;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.Recommendation;
import com.resumescreening.api.service.ApplicationService;
import com.resumescreening.api.service.JobPostingService;
import com.resumescreening.api.service.ScreeningService;
import com.resumescreening.api.service.UserService;
import com.resumescreening.api.util.DtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get application and validate ownership through job posting
        Application application = applicationService.getApplicationById(request.getApplicationId());

        if (!application.getJobPosting().getUser().getId().equals(user.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You don't have permission to screen this application"));
        }

        // Screen application
        ScreeningResult result = screeningService.getScreeningResultByApplicationId(application.getId())
                .orElseGet(() -> screeningService.screenApplication(application));

        ScreeningResultResponse response = DtoMapper.toScreeningResultResponse(result);
        String message = result.getId() != null && result.getCreatedAt() != null
                ? "Application already screened - returning existing result"
                : "Application screened successfully";
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, response));
    }

    // ✅ Batch screen all applications for a job
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<List<ScreeningResultResponse>>> batchScreenApplications(
            @Valid @RequestBody BatchScreeningRequest request,
            Authentication authentication
    ) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get job and validate ownership
        JobPosting job = jobPostingService.getJobById(request.getJobPostingId());
        if (!job.getUser().getId().equals(user.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You don't have permission to screen for this job"));
        }

        // Batch screen all applications for this job
        List<ScreeningResult> results = screeningService.batchScreenApplications(request.getJobPostingId(), user);

        List<ScreeningResultResponse> responses = results.stream()
                .map(DtoMapper::toScreeningResultResponse)
                .collect(Collectors.toList());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Batch screening completed", responses));
    }

    // Get screening result by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScreeningResultResponse>> getScreeningResult(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ScreeningResult result = screeningService.getScreeningResult(id);

        // Access job posting through application
        if (!result.getApplication().getJobPosting().getUser().getId().equals(user.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You don't have permission to view this screening result"));
        }

        ScreeningResultResponse response = DtoMapper.toScreeningResultResponse(result);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Get all screening results for a job
    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<List<ScreeningResultResponse>>> getJobScreeningResults(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate job ownership
        JobPosting job = jobPostingService.getJobById(jobId);
        if (!job.getUser().getId().equals(user.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You don't have permission to view screening results for this job"));
        }

        List<ScreeningResult> results = screeningService.getScreeningResultsByJobId(jobId);

        List<ScreeningResultResponse> responses = results.stream()
                .map(DtoMapper::toScreeningResultResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // ✅ Get candidates by recommendation level
    @GetMapping("/job/{jobId}/recommendation/{recommendation}")
    public ResponseEntity<ApiResponse<List<ScreeningResultResponse>>> getCandidatesByRecommendation(
            @PathVariable Long jobId,
            @PathVariable Recommendation recommendation,
            Authentication authentication
    ) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate job ownership
        JobPosting job = jobPostingService.getJobById(jobId);
        if (!job.getUser().getId().equals(user.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        List<ScreeningResult> results = screeningService.getCandidatesByRecommendation(jobId, recommendation);

        List<ScreeningResultResponse> responses = results.stream()
                .map(DtoMapper::toScreeningResultResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // ✅ NEW: Get screening statistics for a job
    @GetMapping("/job/{jobId}/stats")
    public ResponseEntity<ApiResponse<?>> getScreeningStats(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate job ownership
        JobPosting job = jobPostingService.getJobById(jobId);
        if (!job.getUser().getId().equals(user.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied"));
        }

        var stats = screeningService.getScreeningStatistics(jobId);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}