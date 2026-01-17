package com.resumescreening.api.repository;

import com.resumescreening.api.model.entity.Application;
import com.resumescreening.api.model.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // Find all applications for a specific job
    List<Application> findByJobPostingId(Long jobPostingId);

    // Find all applications by a candidate
    List<Application> findByCandidateId(Long candidateId);

    // Check if candidate already applied to a job
    boolean existsByJobPostingIdAndCandidateId(Long jobPostingId, Long candidateId);

    // Find applications by job and status
    List<Application> findByJobPostingIdAndStatus(Long jobPostingId, ApplicationStatus status);

    // Paginated applications for a job
    Page<Application> findByJobPostingId(Long jobPostingId, Pageable pageable);

    // Find application with screening result
    @Query("SELECT a FROM Application a LEFT JOIN FETCH a.screeningResult WHERE a.id = :id")
    Optional<Application> findByIdWithScreeningResult(@Param("id") Long id);

    // Count applications for a job
    long countByJobPostingId(Long jobPostingId);

    // Count applications by status for a job
    long countByJobPostingIdAndStatus(Long jobPostingId, ApplicationStatus status);
}