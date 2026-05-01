package com.resumescreening.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumescreening.api.model.entity.Resume;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.repository.ResumeRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResumeServiceTest {

    private final ResumeRepository resumeRepository = mock(ResumeRepository.class);
    private final ResumeService resumeService = new ResumeService(
            resumeRepository,
            mock(UserService.class),
            mock(FileStorageService.class),
            mock(ResumeParserService.class),
            new ObjectMapper()
    );

    @Test
    void getResumeByIdRejectsNonOwner() {
        Resume resume = new Resume();
        resume.setId(10L);
        User owner = new User();
        owner.setId(1L);
        resume.setUser(owner);

        when(resumeRepository.findByIdWithUser(10L)).thenReturn(Optional.of(resume));

        assertThatThrownBy(() -> resumeService.getResumeById(10L, 99L))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void deleteResumeRejectsNonOwner() {
        Resume resume = new Resume();
        resume.setId(10L);
        resume.setFilePath("s3://bucket/resume.pdf");
        User owner = new User();
        owner.setId(1L);
        resume.setUser(owner);

        when(resumeRepository.findByIdWithUser(10L)).thenReturn(Optional.of(resume));

        assertThatThrownBy(() -> resumeService.deleteResume(10L, 99L))
                .isInstanceOf(SecurityException.class);
    }
}
