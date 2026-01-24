package com.resumescreening.api.controller;

import com.resumescreening.api.model.dto.request.CreateJobRequest;
import com.resumescreening.api.model.dto.request.UpdateJobRequest;
import com.resumescreening.api.model.dto.response.ApiResponse;
import com.resumescreening.api.model.dto.response.JobPostingResponse;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.service.JobPostingService;
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
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobPostingController {

    private final JobPostingService jobPostingService;
    private final UserService userService;

    // GET /api/v1/jobs - List all active jobs (public)
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
        Page<JobPostingResponse> jobsPage = jobPostingService.getAllActiveJobs(pageable);

        return ResponseEntity.ok(ApiResponse.success(jobsPage));
    }

    // GET /api/v1/jobs/{id} - Get single job (public)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobPostingResponse>> getJobById(@PathVariable Long id) {
        JobPostingResponse job = jobPostingService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.success(job));
    }

    // GET /api/v1/jobs/search - Search jobs with filters
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<JobPostingResponse>>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) String employmentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<JobPostingResponse> jobsPage = jobPostingService.searchJobs(
                keyword, location, experienceLevel, employmentType, pageable
        );

        return ResponseEntity.ok(ApiResponse.success(jobsPage));
    }

    // GET /api/v1/jobs/my-jobs - Get recruiter's own jobs
    @GetMapping("/my-jobs")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<List<JobPostingResponse>>> getMyJobs(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        List<JobPostingResponse> responses = jobPostingService.getActiveJobsByUser(user.getId());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // POST /api/v1/jobs - Create job
    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<JobPostingResponse>> createJob(
            @Valid @RequestBody CreateJobRequest request,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);
        JobPostingResponse job = jobPostingService.createJob(
                user.getId(), request.getTitle(), request.getDescription(),
                request.getRequiredSkills(), request.getExperienceLevel(),
                request.getEmploymentType(), request.getLocation(),
                request.getSalaryRange(), request.getCompanyName()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Job created successfully", job));
    }

    // PUT /api/v1/jobs/{id} - Update job
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<JobPostingResponse>> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobRequest request,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);
        JobPostingResponse job = jobPostingService.updateJob(
                id, user.getId(), request.getTitle(), request.getDescription(),
                request.getRequiredSkills(), request.getExperienceLevel(),
                request.getEmploymentType(), request.getLocation(), request.getSalaryRange()
        );

        return ResponseEntity.ok(ApiResponse.success("Job updated successfully", job));
    }

    // PATCH /api/v1/jobs/{id}/deactivate - Deactivate job
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Void>> deactivateJob(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);
        jobPostingService.deactivateJob(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Job deactivated successfully", null));
    }

    // DELETE /api/v1/jobs/{id} - Delete job
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);
        jobPostingService.deleteJob(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Job deleted successfully", null));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}