package com.resumescreening.api.repository;

import com.resumescreening.api.model.entity.JobPosting;
import com.resumescreening.api.model.enums.ExperienceLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    // Find all jobs by user (for recruiter dashboard)
    List<JobPosting> findByUserId(Long userId);

    // Find active jobs by user
    List<JobPosting> findByUserIdAndIsActiveTrue(Long userId);

    // Paginated active jobs
    Page<JobPosting> findByIsActiveTrue(Pageable pageable);

    // Find by experience level
    List<JobPosting> findByExperienceLevel(ExperienceLevel level);

    // Custom query - find jobs by skill (searches in array)
    @Query("SELECT j FROM JobPosting j WHERE :skill MEMBER OF j.requiredSkills")
    List<JobPosting> findByRequiredSkillsContaining(@Param("skill") String skill);

    // Count active jobs for a user
    long countByUserIdAndIsActiveTrue(Long userId);

    // Find with screening results loaded (optimization)
    @Query("SELECT DISTINCT j FROM JobPosting j LEFT JOIN FETCH j.screeningResults WHERE j.id = :id")
    Optional<JobPosting> findByIdWithScreeningResults(@Param("id") Long id);
}