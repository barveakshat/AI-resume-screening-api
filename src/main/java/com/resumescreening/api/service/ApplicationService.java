package com.resumescreening.api.service;

import com.resumescreening.api.exception.ApplicationAlreadyExistsException;
import com.resumescreening.api.exception.ResourceNotFoundException;
import com.resumescreening.api.exception.UnauthorizedException;
import com.resumescreening.api.model.entity.Application;
import com.resumescreening.api.model.entity.JobPosting;
import com.resumescreening.api.model.entity.Resume;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.ApplicationStatus;
import com.resumescreening.api.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobPostingService jobPostingService;
    private final ResumeService resumeService;

    @Transactional
    public Application applyToJob(Long jobId, Long resumeId, String coverLetter, User candidate) {
        // Validate job exists and is active
        JobPosting job = jobPostingService.getJobById(jobId);
        if (!job.getIsActive()) {
            throw new IllegalStateException("This job posting is no longer active");
        }

        // Validate resume belongs to candidate
        Resume resume = resumeService.getResumeById(resumeId);
        if (!resume.getUser().getId().equals(candidate.getId())) {
            throw new UnauthorizedException("You can only apply with your own resumes");
        }

        // Check if already applied
        if (applicationRepository.existsByJobPostingIdAndCandidateId(jobId, candidate.getId())) {
            throw new ApplicationAlreadyExistsException("You have already applied to this job");
        }

        // Create application
        Application application = Application.builder()
                .jobPosting(job)
                .candidate(candidate)
                .resume(resume)
                .coverLetter(coverLetter)
                .status(ApplicationStatus.PENDING)
                .build();

        return applicationRepository.save(application);
    }

    public List<Application> getApplicationsForJob(Long jobId, User recruiter) {
        JobPosting job = jobPostingService.getJobById(jobId);

        // Verify recruiter owns the job
        if (!job.getUser().getId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only view applications for your own jobs");
        }

        return applicationRepository.findByJobPostingId(jobId);
    }

    public Page<Application> getApplicationsForJobPaginated(Long jobId, User recruiter, Pageable pageable) {
        JobPosting job = jobPostingService.getJobById(jobId);

        if (!job.getUser().getId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only view applications for your own jobs");
        }

        return applicationRepository.findByJobPostingId(jobId, pageable);
    }

    public List<Application> getMyCandidateApplications(User candidate) {
        return applicationRepository.findByCandidateId(candidate.getId());
    }

    public Application getApplicationById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
    }

    public Application getApplicationWithScreeningResult(Long id) {
        return applicationRepository.findByIdWithScreeningResult(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
    }

    @Transactional
    public Application updateApplicationStatus(Long applicationId, ApplicationStatus status, User recruiter) {
        Application application = getApplicationById(applicationId);

        // Verify recruiter owns the job
        if (!application.getJobPosting().getUser().getId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only update applications for your own jobs");
        }

        application.setStatus(status);
        return applicationRepository.save(application);
    }

    @Transactional
    public void withdrawApplication(Long applicationId, User candidate) {
        Application application = getApplicationById(applicationId);

        // Verify candidate owns the application
        if (!application.getCandidate().getId().equals(candidate.getId())) {
            throw new UnauthorizedException("You can only withdraw your own applications");
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        applicationRepository.save(application);
    }

    public long countApplicationsForJob(Long jobId) {
        return applicationRepository.countByJobPostingId(jobId);
    }

    public List<Application> getApplicationsByStatus(Long jobId, ApplicationStatus status, User recruiter) {
        JobPosting job = jobPostingService.getJobById(jobId);

        if (!job.getUser().getId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You can only view applications for your own jobs");
        }

        return applicationRepository.findByJobPostingIdAndStatus(jobId, status);
    }
}