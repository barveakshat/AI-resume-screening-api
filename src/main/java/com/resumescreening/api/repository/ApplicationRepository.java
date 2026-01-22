package com.resumescreening.api.repository;

import com.resumescreening.api.model.entity.Application;
import com.resumescreening.api.model.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

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

    // Count applications for a job
    long countByJobPostingId(Long jobPostingId);
}