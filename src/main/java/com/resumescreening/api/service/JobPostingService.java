package com.resumescreening.api.service;

import com.resumescreening.api.exception.ResourceNotFoundException;
import com.resumescreening.api.model.dto.response.JobPostingResponse;
import com.resumescreening.api.model.entity.JobPosting;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.EmploymentType;
import com.resumescreening.api.model.enums.ExperienceLevel;
import com.resumescreening.api.repository.JobPostingRepository;
import com.resumescreening.api.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @Transactional
    @CacheEvict(value = {"activeJobsList", "userJobs"}, allEntries = true)
    public JobPostingResponse createJob(Long userId, String title, String description,
                                        List<String> requiredSkills, ExperienceLevel experienceLevel,
                                        EmploymentType employmentType, String location, String salaryRange, String companyName) {
        User user = userService.getUserById(userId);
        JobPosting job = new JobPosting();
        job.setUser(user);
        job.setTitle(title);
        job.setDescription(description);
        job.setRequiredSkills(requiredSkills);
        job.setExperienceLevel(experienceLevel);
        job.setEmploymentType(employmentType);
        job.setLocation(location);
        job.setSalaryRange(salaryRange);
        job.setCompanyName(companyName);
        job.setIsActive(true);
        job = jobPostingRepository.save(job);

        // Eagerly fetch user before transaction ends
        job.getUser().getFullName();

        log.info("Job posting created: {} by user {}", job.getId(), userId);
        return DtoMapper.toJobPostingResponse(job);
    }

    @Cacheable(value = "jobs", key = "#jobId")
    public JobPostingResponse getJobById(Long jobId) {
        JobPosting job = jobPostingRepository.findByIdWithUser(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));
        return DtoMapper.toJobPostingResponse(job);
    }

    // Internal method to get entity (for ownership validation)
    public JobPosting getJobEntityById(Long jobId) {
        return jobPostingRepository.findByIdWithUser(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));
    }

    @Cacheable(value = "userJobs", key = "#userId")
    public List<JobPostingResponse> getActiveJobsByUser(Long userId) {
        List<JobPosting> jobs = jobPostingRepository.findByUserIdAndIsActiveTrue(userId);
        return jobs.stream()
                .map(DtoMapper::toJobPostingResponse)
                .toList();
    }

    // DON'T cache paginated results
    public Page<JobPostingResponse> getAllActiveJobs(Pageable pageable) {
        Page<JobPosting> jobsPage = jobPostingRepository.findByIsActiveTrue(pageable);
        return jobsPage.map(DtoMapper::toJobPostingResponse);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "jobs", key = "#jobId"),
            @CacheEvict(value = "activeJobsList", allEntries = true),
            @CacheEvict(value = "userJobs", key = "#userId")
    })
    public JobPostingResponse updateJob(Long jobId, Long userId, String title, String description,
                                        List<String> requiredSkills, ExperienceLevel experienceLevel,
                                        EmploymentType employmentType, String location, String salaryRange) {

        JobPosting job = getJobEntityById(jobId);
        validateOwnership(job, userId);

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

        JobPosting updatedJob = jobPostingRepository.save(job);
        Hibernate.initialize(updatedJob.getUser());
        log.info("Job posting updated: {}", jobId);
        return DtoMapper.toJobPostingResponse(updatedJob);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "jobs", key = "#jobId"),
            @CacheEvict(value = "activeJobsList", allEntries = true),
            @CacheEvict(value = "userJobs", key = "#userId")
    })
    public void deactivateJob(Long jobId, Long userId) {
        JobPosting job = getJobEntityById(jobId);
        validateOwnership(job, userId);

        job.setIsActive(false);
        jobPostingRepository.save(job);

        log.info("Job posting deactivated: {}", jobId);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "jobs", key = "#jobId"),
            @CacheEvict(value = "activeJobsList", allEntries = true),
            @CacheEvict(value = "userJobs", key = "#userId")
    })
    public void deleteJob(Long jobId, Long userId) {
        JobPosting job = getJobEntityById(jobId);
        validateOwnership(job, userId);

        jobPostingRepository.delete(job);

        log.info("Job posting deleted: {}", jobId);
    }

    private void validateOwnership(JobPosting job, Long userId) {
        if (!job.getUser().getId().equals(userId)) {
            throw new SecurityException("You don't have permission to modify this job posting");
        }
    }

    // DON'T cache search results - they're dynamic
    public Page<JobPostingResponse> searchJobs(String keyword, String location, String experienceLevel,
                                               String employmentType, Pageable pageable) {

        String expLevel = null;
        String empType = null;

        if (experienceLevel != null && !experienceLevel.trim().isEmpty()) {
            expLevel = experienceLevel.trim().toUpperCase();
        }
        if (employmentType != null && !employmentType.trim().isEmpty()) {
            empType = employmentType.trim().toUpperCase();
        }

        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<JobPosting> jobsPage = jobPostingRepository.searchJobs(keyword, location, expLevel, empType, unsortedPageable);

        return jobsPage.map(DtoMapper::toJobPostingResponse);
    }
}