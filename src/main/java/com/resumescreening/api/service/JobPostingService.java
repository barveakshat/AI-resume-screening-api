package com.resumescreening.api.service;

import com.resumescreening.api.exception.ResourceNotFoundException;
import com.resumescreening.api.model.entity.JobPosting;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.EmploymentType;
import com.resumescreening.api.model.enums.ExperienceLevel;
import com.resumescreening.api.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final UserService userService;

    // Create job posting
    @Transactional
    public JobPosting createJob(Long userId, String title, String description,
                                List<String> requiredSkills, ExperienceLevel experienceLevel,
                                EmploymentType employmentType, String location, String salaryRange) {

        User user = userService.getUserById(userId);

        // Create job posting
        JobPosting job = new JobPosting();
        job.setUser(user);
        job.setTitle(title);
        job.setDescription(description);
        job.setRequiredSkills(requiredSkills);
        job.setExperienceLevel(experienceLevel);
        job.setEmploymentType(employmentType);
        job.setLocation(location);
        job.setSalaryRange(salaryRange);
        job.setIsActive(true);

        job = jobPostingRepository.save(job);

        log.info("Job posting created: {} by user {}", job.getId(), userId);
        return job;
    }

    // Get job by ID
    public JobPosting getJobById(Long jobId) {
        return jobPostingRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));
    }

    // Get all jobs for a user
    public List<JobPosting> getJobsByUser(Long userId) {
        return jobPostingRepository.findByUserId(userId);
    }

    // Get active jobs for a user
    public List<JobPosting> getActiveJobsByUser(Long userId) {
        return jobPostingRepository.findByUserIdAndIsActiveTrue(userId);
    }

    // Get all active jobs (paginated)
    public Page<JobPosting> getAllActiveJobs(Pageable pageable) {
        return jobPostingRepository.findByIsActiveTrue(pageable);
    }

    // Update job posting
    @Transactional
    public JobPosting updateJob(Long jobId, Long userId, String title, String description,
                                List<String> requiredSkills, ExperienceLevel experienceLevel,
                                EmploymentType employmentType, String location, String salaryRange) {

        JobPosting job = getJobById(jobId);

        // Validate ownership
        validateOwnership(job, userId);

        // Update fields
        if (title != null && !title.trim().isEmpty()) {
            job.setTitle(title);
        }
        if (description != null && !description.trim().isEmpty()) {
            job.setDescription(description);
        }
        if (requiredSkills != null && !requiredSkills.isEmpty()) {
            job.setRequiredSkills(requiredSkills);
        }
        if (experienceLevel != null) {
            job.setExperienceLevel(experienceLevel);
        }
        if (employmentType != null) {
            job.setEmploymentType(employmentType);
        }
        if (location != null) {
            job.setLocation(location);
        }
        if (salaryRange != null) {
            job.setSalaryRange(salaryRange);
        }

        log.info("Job posting updated: {}", jobId);
        return jobPostingRepository.save(job);
    }

    // Deactivate job
    @Transactional
    public void deactivateJob(Long jobId, Long userId) {
        JobPosting job = getJobById(jobId);
        validateOwnership(job, userId);

        job.setIsActive(false);
        jobPostingRepository.save(job);

        log.info("Job posting deactivated: {}", jobId);
    }

    // Delete job
    @Transactional
    public void deleteJob(Long jobId, Long userId) {
        JobPosting job = getJobById(jobId);
        validateOwnership(job, userId);

        jobPostingRepository.delete(job);

        log.info("Job posting deleted: {}", jobId);
    }

    // Count active jobs for user
    public long countActiveJobsForUser(Long userId) {
        return jobPostingRepository.countByUserIdAndIsActiveTrue(userId);
    }

    // Search jobs by skill
    public List<JobPosting> findJobsBySkill(String skill) {
        return jobPostingRepository.findByRequiredSkill(skill);
    }

    // Validate job ownership
    private void validateOwnership(JobPosting job, Long userId) {
        if (!job.getUser().getId().equals(userId)) {
            throw new SecurityException("You don't have permission to modify this job posting");
        }
    }
}