package com.resumescreening.api.service;

import com.resumescreening.api.exception.ApplicationAlreadyExistsException;
import com.resumescreening.api.exception.ResourceNotFoundException;
import com.resumescreening.api.exception.UnauthorizedException;
import com.resumescreening.api.model.dto.response.ApplicationResponse;
import com.resumescreening.api.model.dto.response.JobPostingResponse;
import com.resumescreening.api.model.entity.Application;
import com.resumescreening.api.model.entity.JobPosting;
import com.resumescreening.api.model.entity.Resume;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.ApplicationStatus;
import com.resumescreening.api.repository.ApplicationRepository;
import com.resumescreening.api.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobPostingService jobPostingService;
    private final ResumeService resumeService;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "candidateApplications", key = "#candidate.id"),
            @CacheEvict(value = "jobApplications", key = "#jobId")
    })
    public ApplicationResponse applyToJob(Long jobId, Long resumeId, String coverLetter, User candidate) {
        JobPostingResponse job = jobPostingService.getJobById(jobId);
        if (!job.getIsActive()) {
            throw new IllegalStateException("This job posting is no longer active");
        }
        Resume resume = resumeService.getResumeEntityById(resumeId);
        if (!resume.getUser().getId().equals(candidate.getId())) {
            throw new UnauthorizedException("You can only apply with your own resumes");
        }
        if (applicationRepository.existsByJobPostingIdAndCandidateId(jobId, candidate.getId())) {
            throw new ApplicationAlreadyExistsException("You have already applied to this job");
        }

        // Get job entity for relationship
        JobPosting jobEntity = jobPostingService.getJobEntityById(jobId);

        Application application = Application.builder()
                .jobPosting(jobEntity)
                .candidate(candidate)
                .resume(resume)
                .coverLetter(coverLetter)
                .status(ApplicationStatus.PENDING)
                .build();

        Application savedApplication = applicationRepository.save(application);

        // Initialize lazy relationships
        Hibernate.initialize(savedApplication.getJobPosting());
        Hibernate.initialize(savedApplication.getCandidate());
        Hibernate.initialize(savedApplication.getResume());

        log.info("Application created: {} for job: {} by candidate: {}",
                savedApplication.getId(), jobId, candidate.getId());

        return DtoMapper.toApplicationResponse(savedApplication);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForJob(Long jobId, User recruiter) {
        JobPostingResponse job = jobPostingService.getJobById(jobId);
        if (!job.getRecruiterId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only view applications for your own jobs");
        }
        List<Application> applications = applicationRepository.findByJobPostingId(jobId);
        return applications.stream()
                .map(DtoMapper::toApplicationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplicationsForJobPaginated(Long jobId, User recruiter, Pageable pageable) {
        JobPostingResponse job = jobPostingService.getJobById(jobId);

        if (!job.getRecruiterId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only view applications for your own jobs");
        }
        Page<Application> applicationPage = applicationRepository.findByJobPostingId(jobId, pageable);
        return applicationPage.map(DtoMapper::toApplicationResponse);
    }

    @Cacheable(value = "candidateApplications", key = "#candidate.id")
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyCandidateApplications(User candidate) {
        List<Application> applications = applicationRepository.findByCandidateId(candidate.getId());
        return applications.stream()
                .map(DtoMapper::toApplicationResponse)
                .toList();
    }

    @Cacheable(value = "applications", key = "#id")
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long id) {
        Application application = getApplicationEntityById(id);
        return DtoMapper.toApplicationResponse(application);
    }

    // Internal method to get entity with relationships initialized
    @Transactional(readOnly = true)
    public Application getApplicationEntityById(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));

        // Initialize relationships to avoid lazy loading issues
        Hibernate.initialize(application.getJobPosting());
        Hibernate.initialize(application.getCandidate());
        Hibernate.initialize(application.getResume());

        return application;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "applications", key = "#applicationId"),
            @CacheEvict(value = "candidateApplications", allEntries = true),
            @CacheEvict(value = "jobApplications", allEntries = true)
    })
    public ApplicationResponse updateApplicationStatus(Long applicationId, ApplicationStatus status, User recruiter) {
        Application application = getApplicationEntityById(applicationId);
        if (!application.getJobPosting().getUser().getId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only update applications for your own jobs");
        }
        application.setStatus(status);
        // Set screenedAt timestamp if status is being changed to UNDER_REVIEW
        if (status == ApplicationStatus.UNDER_REVIEW && application.getScreenedAt() == null) {
            application.setScreenedAt(LocalDateTime.now());
        }
        Application updatedApplication = applicationRepository.save(application);

        log.info("Application status updated: {} to status: {} by recruiter: {}",
                applicationId, status, recruiter.getId());
        return DtoMapper.toApplicationResponse(updatedApplication);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "applications", key = "#applicationId"),
            @CacheEvict(value = "candidateApplications", key = "#candidate.id")
    })
    public void withdrawApplication(Long applicationId, User candidate) {
        Application application = getApplicationEntityById(applicationId);
        if (!application.getCandidate().getId().equals(candidate.getId())) {
            throw new UnauthorizedException("You can only withdraw your own applications");
        }
        application.setStatus(ApplicationStatus.WITHDRAWN);
        applicationRepository.save(application);

        log.info("Application withdrawn: {} by candidate: {}", applicationId, candidate.getId());
    }

    // Simple count - no caching needed
    public long countApplicationsForJob(Long jobId) {
        return applicationRepository.countByJobPostingId(jobId);
    }

    // Not cached - filtered results
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByStatus(Long jobId, ApplicationStatus status, User recruiter) {
        JobPostingResponse job = jobPostingService.getJobById(jobId);

        if (!job.getRecruiterId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only view applications for your own jobs");
        }

        List<Application> applications = applicationRepository.findByJobPostingIdAndStatus(jobId, status);
        return applications.stream()
                .map(DtoMapper::toApplicationResponse)
                .toList();
    }
}