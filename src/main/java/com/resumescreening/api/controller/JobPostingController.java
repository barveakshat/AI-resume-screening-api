package com.resumescreening.api.controller;

import com.resumescreening.api.model.dto.request.CreateJobRequest;
import com.resumescreening.api.model.dto.request.UpdateJobRequest;
import com.resumescreening.api.model.dto.response.ApiResponse;
import com.resumescreening.api.model.dto.response.JobPostingResponse;
import com.resumescreening.api.model.entity.JobPosting;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.service.JobPostingService;
import com.resumescreening.api.service.UserService;
import com.resumescreening.api.util.DtoMapper;
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
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService jobPostingService;
    private final UserService userService;

    // Create job posting (Recruiters only)
    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<JobPostingResponse>> createJob(
            @Valid @RequestBody CreateJobRequest request,
            Authentication authentication
    ) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        JobPosting job = jobPostingService.createJob(
                user.getId(),
                request.getTitle(),
                request.getDescription(),
                request.getRequiredSkills(),
                request.getExperienceLevel(),
                request.getEmploymentType(),
                request.getLocation(),
                request.getSalaryRange()
        );

        JobPostingResponse response = DtoMapper.toJobPostingResponse(job);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Job posting created successfully", response));
    }

    // Get all jobs for current user (Recruiters only)
    @GetMapping("/my-jobs")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<List<JobPostingResponse>>> getMyJobs(
            Authentication authentication
    ) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<JobPosting> jobs = jobPostingService.getActiveJobsByUser(user.getId());

        List<JobPostingResponse> responses = jobs.stream()
                .map(DtoMapper::toJobPostingResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success(responses)
        );
    }

    // Get all active jobs (paginated) - anyone can view
    @GetMapping
    public ResponseEntity<ApiResponse<Page<JobPostingResponse>>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<JobPosting> jobsPage = jobPostingService.getAllActiveJobs(pageable);

        Page<JobPostingResponse> responsePage = jobsPage.map(DtoMapper::toJobPostingResponse);

        return ResponseEntity.ok(
                ApiResponse.success(responsePage)
        );
    }

    // Get job by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobPostingResponse>> getJobById(
            @PathVariable Long id
    ) {
        JobPosting job = jobPostingService.getJobById(id);
        JobPostingResponse response = DtoMapper.toJobPostingResponse(job);

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    // Update job (Recruiters only, owner only)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<JobPostingResponse>> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobRequest request,
            Authentication authentication
    ) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        JobPosting job = jobPostingService.updateJob(
                id,
                user.getId(),
                request.getTitle(),
                request.getDescription(),
                request.getRequiredSkills(),
                request.getExperienceLevel(),
                request.getEmploymentType(),
                request.getLocation(),
                request.getSalaryRange()
        );

        JobPostingResponse response = DtoMapper.toJobPostingResponse(job);

        return ResponseEntity.ok(
                ApiResponse.success("Job updated successfully", response)
        );
    }

    // Deactivate job (Recruiters only, owner only)
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Void>> deactivateJob(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        jobPostingService.deactivateJob(id, user.getId());

        return ResponseEntity.ok(
                ApiResponse.success("Job deactivated successfully", null)
        );
    }

    // Delete job (Recruiters only, owner only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        jobPostingService.deleteJob(id, user.getId());

        return ResponseEntity.ok(
                ApiResponse.success("Job deleted successfully", null)
        );
    }
}