package com.resumescreening.api.repository;

import com.resumescreening.api.model.dto.response.JobPostingResponse;
import com.resumescreening.api.model.entity.JobPosting;
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

    // Find active jobs by user
    List<JobPosting> findByUserIdAndIsActiveTrue(Long userId);

    // Paginated active jobs
    Page<JobPosting> findByIsActiveTrue(Pageable pageable);

    @Query(value = """
    SELECT * FROM job_postings jp
    WHERE jp.is_active = true
    AND (:keyword IS NULL OR LOWER(jp.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
         OR LOWER(jp.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
    AND (:location IS NULL OR LOWER(jp.location) LIKE LOWER(CONCAT('%', :location, '%')))
    AND (:experienceLevel IS NULL OR CAST(jp.experience_level AS VARCHAR) = :experienceLevel)
    AND (:employmentType IS NULL OR CAST(jp.employment_type AS VARCHAR) = :employmentType)
    """, nativeQuery = true)
    Page<JobPosting> searchJobs(  // Returns entities, not DTOs
                                  @Param("keyword") String keyword,
                                  @Param("location") String location,
                                  @Param("experienceLevel") String experienceLevel,
                                  @Param("employmentType") String employmentType,
                                  Pageable pageable
    );
    @Query("SELECT j FROM JobPosting j LEFT JOIN FETCH j.user WHERE j.id = :id")
    Optional<JobPosting> findByIdWithUser(@Param("id") Long id);
}