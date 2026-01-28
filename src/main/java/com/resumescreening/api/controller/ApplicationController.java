package com.resumescreening.api.controller;

import com.resumescreening.api.model.dto.request.ApplyJobRequest;
import com.resumescreening.api.model.dto.response.ApiResponse;
import com.resumescreening.api.model.dto.response.ApplicationResponse;
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

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserService userService;

    // CANDIDATE: Apply to a job
    @PostMapping("/job/{jobId}/apply")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> applyToJob(
            @PathVariable Long jobId,
            @Valid @RequestBody ApplyJobRequest request,
            Authentication authentication
    ) {
        User candidate = getAuthenticatedUser(authentication);

        ApplicationResponse applicationResponse = applicationService.applyToJob(
                jobId,
                request.getResumeId(),
                request.getCoverLetter(),
                candidate
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application submitted successfully", applicationResponse));
    }

    // CANDIDATE: View my applications
    @GetMapping("/my-applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getMyApplications(
            Authentication authentication
    ) {
        User candidate = getAuthenticatedUser(authentication);

        List<ApplicationResponse> applications = applicationService.getMyCandidateApplications(candidate);

        return ResponseEntity.ok(ApiResponse.success(applications));
    }

    // CANDIDATE: Withdraw application
    @PatchMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<Void>> withdrawApplication(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User candidate = getAuthenticatedUser(authentication);

        applicationService.withdrawApplication(id, candidate);

        return ResponseEntity.ok(ApiResponse.success("Application withdrawn successfully", null));
    }

    // RECRUITER: View all applications for their job (with pagination)
    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<?>> getApplicationsForJob(
            @PathVariable Long jobId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "appliedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Authentication authentication
    ) {
        User recruiter = getAuthenticatedUser(authentication);

        if (status != null) {
            // If filtering by status, return a list
            List<ApplicationResponse> applications =
                    applicationService.getApplicationsByStatus(jobId, status, recruiter);
            return ResponseEntity.ok(ApiResponse.success(applications));
        } else {
            // Return paginated results
            Sort sort = sortDir.equalsIgnoreCase("ASC")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<ApplicationResponse> applicationPage =
                    applicationService.getApplicationsForJobPaginated(jobId, recruiter, pageable);
            return ResponseEntity.ok(ApiResponse.success(applicationPage));
        }
    }

    // RECRUITER: Update application status
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status,
            Authentication authentication
    ) {
        User recruiter = getAuthenticatedUser(authentication);

        ApplicationResponse response = applicationService.updateApplicationStatus(id, status, recruiter);

        return ResponseEntity.ok(ApiResponse.success("Application status updated", response));
    }

    // RECRUITER: Get application count for job
    @GetMapping("/job/{jobId}/count")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Long>> getApplicationCount(
            @PathVariable Long jobId,
            Authentication authentication
    ) {
        getAuthenticatedUser(authentication);
        long count = applicationService.countApplicationsForJob(jobId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // Get single application details
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'RECRUITER')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplicationDetails(
            @PathVariable Long id,
            Authentication authentication
    ) {
        getAuthenticatedUser(authentication);

        ApplicationResponse response = applicationService.getApplicationById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}