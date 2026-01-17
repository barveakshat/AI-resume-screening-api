package com.resumescreening.api.controller;

import com.resumescreening.api.model.dto.request.ApplyJobRequest;
import com.resumescreening.api.model.dto.response.ApiResponse;
import com.resumescreening.api.model.dto.response.ApplicationResponse;
import com.resumescreening.api.util.DtoMapper;
import com.resumescreening.api.model.entity.Application;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.ApplicationStatus;
import com.resumescreening.api.service.ApplicationService;
import com.resumescreening.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserService userService;

    // CANDIDATE: Apply to a job
    @PostMapping("/apply")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> applyToJob(
            @Valid @RequestBody ApplyJobRequest request,
            Authentication authentication
    ) {
        User candidate = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Application application = applicationService.applyToJob(
                request.getJobId(),
                request.getResumeId(),
                request.getCoverLetter(),
                candidate
        );

        ApplicationResponse response = DtoMapper.toApplicationResponse(application);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application submitted successfully", response));
    }

    // CANDIDATE: View my applications
    @GetMapping("/my-applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getMyApplications(
            Authentication authentication
    ) {
        User candidate = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ApplicationResponse> applications = applicationService
                .getMyCandidateApplications(candidate)
                .stream()
                .map(DtoMapper::toApplicationResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(applications));
    }

    // CANDIDATE: Withdraw application
    @PatchMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<String>> withdrawApplication(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User candidate = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        applicationService.withdrawApplication(id, candidate);

        return ResponseEntity.ok(ApiResponse.success(null, "Application withdrawn successfully"));
    }

    // RECRUITER: View all applications for their job
    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplicationsForJob(
            @PathVariable Long jobId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        User recruiter = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Application> applications;
        if (status != null) {
            applications = applicationService.getApplicationsByStatus(jobId, status, recruiter);
        } else {
            Pageable pageable = PageRequest.of(page, size, Sort.by("appliedAt").descending());
            Page<Application> applicationPage = applicationService.getApplicationsForJobPaginated(jobId, recruiter, pageable);
            applications = applicationPage.getContent();
        }

        List<ApplicationResponse> responses = applications.stream()
                .map(DtoMapper::toApplicationResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // RECRUITER: Update application status
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status,
            Authentication authentication
    ) {
        User recruiter = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Application application = applicationService.updateApplicationStatus(id, status, recruiter);
        ApplicationResponse response = DtoMapper.toApplicationResponse(application);

        return ResponseEntity.ok(ApiResponse.success("Application status updated", response));
    }

    // RECRUITER: Get application count for job
    @GetMapping("/job/{jobId}/count")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Long>> getApplicationCount(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        long count = applicationService.countApplicationsForJob(jobId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}