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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobPostingService jobPostingService;
    private final ResumeService resumeService;

    @Transactional
    public ApplicationResponse applyToJob(Long jobId, Long resumeId, String coverLetter, User candidate) {
        // Validate job exists and is active
        JobPostingResponse job = jobPostingService.getJobById(jobId);
        if (!job.getIsActive()) {
            throw new IllegalStateException("This job posting is no longer active");
        }

        // Validate resume belongs to candidate - USE ENTITY METHOD
        Resume resume = resumeService.getResumeEntityById(resumeId);
        if (!resume.getUser().getId().equals(candidate.getId())) {
            throw new UnauthorizedException("You can only apply with your own resumes");
        }

        // Check if already applied
        if (applicationRepository.existsByJobPostingIdAndCandidateId(jobId, candidate.getId())) {
            throw new ApplicationAlreadyExistsException("You have already applied to this job");
        }

        // Get job entity for relationship
        JobPosting jobEntity = jobPostingService.getJobEntityById(jobId);

        // Create application
        Application application = Application.builder()
                .jobPosting(jobEntity)
                .candidate(candidate)
                .resume(resume)
                .coverLetter(coverLetter)
                .status(ApplicationStatus.PENDING)
                .build();

        Application savedApplication = applicationRepository.save(application);
        log.info("Application created: {} for job: {} by candidate: {}",
                savedApplication.getId(), jobId, candidate.getId());

        return DtoMapper.toApplicationResponse(savedApplication);
    }

    public List<ApplicationResponse> getApplicationsForJob(Long jobId, User recruiter) {
        JobPostingResponse job = jobPostingService.getJobById(jobId);

        // Verify recruiter owns the job
        if (!job.getUser().getId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only view applications for your own jobs");
        }

        List<Application> applications = applicationRepository.findByJobPostingId(jobId);
        return applications.stream()
                .map(DtoMapper::toApplicationResponse)
                .toList();
    }

    public Page<ApplicationResponse> getApplicationsForJobPaginated(Long jobId, User recruiter, Pageable pageable) {
        JobPostingResponse job = jobPostingService.getJobById(jobId);

        if (!job.getUser().getId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only view applications for your own jobs");
        }

        Page<Application> applicationPage = applicationRepository.findByJobPostingId(jobId, pageable);
        return applicationPage.map(DtoMapper::toApplicationResponse);
    }

    public List<ApplicationResponse> getMyCandidateApplications(User candidate) {
        List<Application> applications = applicationRepository.findByCandidateId(candidate.getId());
        return applications.stream()
                .map(DtoMapper::toApplicationResponse)
                .toList();
    }

    public ApplicationResponse getApplicationById(Long id) {
        Application application = getApplicationEntityById(id);
        return DtoMapper.toApplicationResponse(application);
    }

    // Internal method to get entity
    public Application getApplicationEntityById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(Long applicationId, ApplicationStatus status, User recruiter) {
        Application application = getApplicationEntityById(applicationId);

        // Verify recruiter owns the job
        if (!application.getJobPosting().getUser().getId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only update applications for your own jobs");
        }

        application.setStatus(status);
        Application updatedApplication = applicationRepository.save(application);

        log.info("Application status updated: {} to status: {} by recruiter: {}",
                applicationId, status, recruiter.getId());

        return DtoMapper.toApplicationResponse(updatedApplication);
    }

    @Transactional
    public void withdrawApplication(Long applicationId, User candidate) {
        Application application = getApplicationEntityById(applicationId);

        // Verify candidate owns the application
        if (!application.getCandidate().getId().equals(candidate.getId())) {
            throw new UnauthorizedException("You can only withdraw your own applications");
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        applicationRepository.save(application);

        log.info("Application withdrawn: {} by candidate: {}", applicationId, candidate.getId());
    }

    public long countApplicationsForJob(Long jobId) {
        return applicationRepository.countByJobPostingId(jobId);
    }

    public List<ApplicationResponse> getApplicationsByStatus(Long jobId, ApplicationStatus status, User recruiter) {
        JobPostingResponse job = jobPostingService.getJobById(jobId);

        if (!job.getUser().getId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only view applications for your own jobs");
        }

        List<Application> applications = applicationRepository.findByJobPostingIdAndStatus(jobId, status);
        return applications.stream()
                .map(DtoMapper::toApplicationResponse)
                .toList();
    }
}