package com.resumescreening.api.service;

import com.resumescreening.api.exception.UnauthorizedException;
import com.resumescreening.api.model.dto.response.ApplicationResponse;
import com.resumescreening.api.model.dto.response.ScreeningStatusResponse;
import com.resumescreening.api.model.entity.Application;
import com.resumescreening.api.model.entity.JobPosting;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.ScreeningStatus;
import com.resumescreening.api.repository.ApplicationRepository;
import com.resumescreening.api.repository.ScreeningResultRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScreeningServiceTest {

    private final ScreeningResultRepository screeningRepository = mock(ScreeningResultRepository.class);
    private final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
    private final ApplicationService applicationService = mock(ApplicationService.class);
    private final ScreeningWorkerService screeningWorkerService = mock(ScreeningWorkerService.class);
    private final ScreeningService screeningService = new ScreeningService(
            screeningRepository,
            applicationRepository,
            applicationService,
            screeningWorkerService
    );

    @Test
    void queueScreeningMarksApplicationQueuedAndStartsWorker() {
        Application application = application(50L, recruiter());
        when(screeningRepository.findByApplicationId(50L)).thenReturn(Optional.empty());
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScreeningStatusResponse response = screeningService.queueScreening(application);

        assertThat(response.getScreeningStatus()).isEqualTo(ScreeningStatus.QUEUED);
        assertThat(application.getScreeningRequestedAt()).isNotNull();
        verify(screeningWorkerService).screenApplicationAsync(50L);
    }

    @Test
    void queueBatchScreeningReturnsQueuedCounts() {
        User recruiter = recruiter();
        Application application = application(50L, recruiter);
        when(applicationService.getApplicationsForJob(10L, recruiter))
                .thenReturn(List.of(ApplicationResponse.builder().id(50L).build()));
        when(applicationService.getApplicationEntityById(50L)).thenReturn(application);
        when(screeningRepository.findByApplicationId(50L)).thenReturn(Optional.empty());
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(screeningService.queueBatchScreening(10L, recruiter).getQueuedCount()).isEqualTo(1);
    }

    @Test
    void getScreeningStatusRejectsUnownedApplication() {
        Application application = application(50L, recruiter());
        when(applicationService.getApplicationEntityById(50L)).thenReturn(application);

        assertThatThrownBy(() -> screeningService.getScreeningStatus(50L, user(99L)))
                .isInstanceOf(UnauthorizedException.class);
    }

    private Application application(Long id, User recruiter) {
        JobPosting job = new JobPosting();
        job.setId(10L);
        job.setUser(recruiter);

        Application application = new Application();
        application.setId(id);
        application.setJobPosting(job);
        application.setScreeningStatus(ScreeningStatus.NOT_STARTED);
        return application;
    }

    private User recruiter() {
        return user(2L);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
