package com.resumescreening.api.repository;

import com.resumescreening.api.model.entity.JobPosting;
import com.resumescreening.api.model.enums.EmploymentType;
import com.resumescreening.api.model.enums.ExperienceLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    // Find active jobs by user
    List<JobPosting> findByUserIdAndIsActiveTrue(Long userId);

    // Paginated active jobs
    Page<JobPosting> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT j FROM JobPosting j WHERE j.isActive = true " +
            "AND (:keyword IS NULL OR (j.title) LIKE (CONCAT('%', :keyword, '%')) " +
            "OR (j.description) LIKE (CONCAT('%', :keyword, '%'))) " +
            "AND (:location IS NULL OR (j.location) LIKE (CONCAT('%', :location, '%'))) " +
            "AND (:experienceLevel IS NULL OR j.experienceLevel = :experienceLevel) " +
            "AND (:employmentType IS NULL OR j.employmentType = :employmentType)")
    Page<JobPosting> searchJobs(
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("experienceLevel") ExperienceLevel experienceLevel,
            @Param("employmentType") EmploymentType employmentType,
            Pageable pageable
    );
}