package com.resumescreening.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumescreening.api.model.entity.Application;
import com.resumescreening.api.model.entity.JobPosting;
import com.resumescreening.api.model.entity.Resume;
import com.resumescreening.api.model.entity.ScreeningResult;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.ScreeningStatus;
import com.resumescreening.api.repository.ApplicationRepository;
import com.resumescreening.api.repository.ScreeningResultRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScreeningWorkerServiceTest {

    private final OpenAIService openAIService = mock(OpenAIService.class);
    private final ScreeningResultRepository screeningRepository = mock(ScreeningResultRepository.class);
    private final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
    private final ScreeningWorkerService workerService = new ScreeningWorkerService(
            openAIService,
            screeningRepository,
            applicationRepository,
            new ObjectMapper()
    );

    @Test
    void screenApplicationAsyncStoresResultAndMarksCompleted() {
        Application application = applicationWithParsedResume("""
                {"fullName":"Ada Lovelace","skills":["Java","Spring"],"totalExperienceYears":3,"education":[]}
                """);
        when(applicationRepository.findByIdWithDetails(50L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(screeningRepository.existsByApplicationId(50L)).thenReturn(false);
        when(openAIService.complete(any())).thenReturn("""
                {"overallScore":82,"skillMatchScore":90,"experienceMatchScore":80,"educationMatchScore":70,
                "matchedSkills":["Java"],"missingSkills":[],"strengths":"Strong Java","weaknesses":"None","summary":"Strong fit","keyHighlights":[]}
                """);
        when(openAIService.cleanJsonResponse(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(screeningRepository.save(any(ScreeningResult.class))).thenAnswer(invocation -> {
            ScreeningResult result = invocation.getArgument(0);
            result.setId(100L);
            return result;
        });

        workerService.screenApplicationAsync(50L).join();

        assertThat(application.getScreeningStatus()).isEqualTo(ScreeningStatus.COMPLETED);
        assertThat(application.getScreeningCompletedAt()).isNotNull();
    }

    @Test
    void screenApplicationAsyncMarksFailedOnInvalidParsedResume() {
        Application application = applicationWithParsedResume(null);
        when(applicationRepository.findByIdWithDetails(50L)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(screeningRepository.existsByApplicationId(50L)).thenReturn(false);

        workerService.screenApplicationAsync(50L).join();

        assertThat(application.getScreeningStatus()).isEqualTo(ScreeningStatus.FAILED);
        assertThat(application.getScreeningError()).contains("Screening failed");
    }

    private Application applicationWithParsedResume(String parsedData) {
        User recruiter = new User();
        recruiter.setId(2L);
        recruiter.setFullName("Recruiter");
        recruiter.setEmail("recruiter@example.com");

        User candidate = new User();
        candidate.setId(1L);
        candidate.setFullName("Candidate");
        candidate.setEmail("candidate@example.com");

        JobPosting job = new JobPosting();
        job.setId(10L);
        job.setTitle("Java Developer");
        job.setDescription("Build APIs");
        job.setRequiredSkills(List.of("Java"));
        job.setUser(recruiter);

        Resume resume = new Resume();
        resume.setId(20L);
        resume.setUser(candidate);
        resume.setParsedData(parsedData);

        Application application = new Application();
        application.setId(50L);
        application.setCandidate(candidate);
        application.setJobPosting(job);
        application.setResume(resume);
        application.setScreeningStatus(ScreeningStatus.QUEUED);
        return application;
    }
}
