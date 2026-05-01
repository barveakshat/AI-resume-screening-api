package com.resumescreening.api.integration;

import com.resumescreening.api.model.entity.JobPosting;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.EmploymentType;
import com.resumescreening.api.model.enums.ExperienceLevel;
import com.resumescreening.api.model.enums.Role;
import com.resumescreening.api.repository.JobPostingRepository;
import com.resumescreening.api.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlywaySchemaIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private Flyway flyway;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void flywayMigrationsApply() {
        assertThat(flyway.info().applied()).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    @Transactional
    void jobRequiredSkillsPersistInNormalizedTable() {
        User recruiter = new User();
        recruiter.setEmail("recruiter@example.com");
        recruiter.setPassword("encoded-password");
        recruiter.setFullName("Recruiter");
        recruiter.setRole(Role.RECRUITER);
        recruiter.setCompanyName("Acme");
        recruiter.setIsActive(true);
        recruiter.setIsEmailVerified(false);
        recruiter = userRepository.saveAndFlush(recruiter);

        JobPosting job = new JobPosting();
        job.setUser(recruiter);
        job.setTitle("Java Developer");
        job.setDescription("Build backend APIs");
        job.setRequiredSkills(List.of("Java", "Spring Boot", "PostgreSQL"));
        job.setExperienceLevel(ExperienceLevel.ENTRY);
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setCompanyName("Acme");
        job.setIsActive(true);
        job = jobPostingRepository.saveAndFlush(job);

        entityManager.clear();

        JobPosting saved = jobPostingRepository.findByIdWithUser(job.getId()).orElseThrow();
        assertThat(saved.getRequiredSkills()).containsExactly("Java", "Spring Boot", "PostgreSQL");
    }
}
