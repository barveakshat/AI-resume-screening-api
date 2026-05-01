package com.resumescreening.api.service;

import com.resumescreening.api.exception.ApplicationAlreadyExistsException;
import com.resumescreening.api.exception.UnauthorizedException;
import com.resumescreening.api.model.dto.response.ApplicationResponse;
import com.resumescreening.api.model.dto.response.JobPostingResponse;
import com.resumescreening.api.model.entity.Application;
import com.resumescreening.api.model.entity.JobPosting;
import com.resumescreening.api.model.entity.Resume;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.Role;
import com.resumescreening.api.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobPostingService jobPostingService;

    @Mock
    private ResumeService resumeService;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    void getApplicationByIdAllowsCandidateOwner() {
        Application application = application(candidate(), recruiter());
        when(applicationRepository.findByIdWithDetails(50L)).thenReturn(Optional.of(application));

        ApplicationResponse response = applicationService.getApplicationById(50L, candidate());

        assertThat(response.getCandidateId()).isEqualTo(1L);
    }

    @Test
    void getApplicationByIdAllowsRecruiterOwner() {
        Application application = application(candidate(), recruiter());
        when(applicationRepository.findByIdWithDetails(50L)).thenReturn(Optional.of(application));

        ApplicationResponse response = applicationService.getApplicationById(50L, recruiter());

        assertThat(response.getJobId()).isEqualTo(10L);
    }

    @Test
    void getApplicationByIdRejectsUnrelatedUser() {
        Application application = application(candidate(), recruiter());
        User other = user(99L, Role.CANDIDATE);
        when(applicationRepository.findByIdWithDetails(50L)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.getApplicationById(50L, other))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void applyToJobRejectsDuplicateCandidateJobApplication() {
        User candidate = candidate();
        Resume resume = new Resume();
        resume.setId(20L);
        resume.setUser(candidate);

        when(jobPostingService.getJobById(10L)).thenReturn(JobPostingResponse.builder()
                .id(10L)
                .isActive(true)
                .build());
        when(resumeService.getResumeEntityById(20L)).thenReturn(resume);
        when(applicationRepository.existsByJobPostingIdAndCandidateId(10L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.applyToJob(10L, 20L, null, candidate))
                .isInstanceOf(ApplicationAlreadyExistsException.class);
    }

    @Test
    void countApplicationsForJobRejectsUnownedJob() {
        when(jobPostingService.getJobById(10L)).thenReturn(JobPostingResponse.builder()
                .id(10L)
                .recruiterId(2L)
                .build());

        assertThatThrownBy(() -> applicationService.countApplicationsForJob(10L, user(99L, Role.RECRUITER)))
                .isInstanceOf(UnauthorizedException.class);
    }

    private Application application(User candidate, User recruiter) {
        JobPosting job = new JobPosting();
        job.setId(10L);
        job.setTitle("Java Developer");
        job.setUser(recruiter);

        Resume resume = new Resume();
        resume.setId(20L);
        resume.setFileName("resume.pdf");
        resume.setUser(candidate);

        Application application = new Application();
        application.setId(50L);
        application.setCandidate(candidate);
        application.setJobPosting(job);
        application.setResume(resume);
        return application;
    }

    private User candidate() {
        return user(1L, Role.CANDIDATE);
    }

    private User recruiter() {
        return user(2L, Role.RECRUITER);
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail("user%s@example.com".formatted(id));
        user.setFullName("User " + id);
        user.setRole(role);
        return user;
    }
}
