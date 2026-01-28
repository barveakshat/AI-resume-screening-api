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

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    @Query("SELECT a FROM Application a " +
            "LEFT JOIN FETCH a.jobPosting " +
            "LEFT JOIN FETCH a.candidate " +
            "LEFT JOIN FETCH a.resume " +
            "WHERE a.jobPosting.id = :jobPostingId")
    List<Application> findByJobPostingId(@Param("jobPostingId") Long jobPostingId);

    @Query("SELECT a FROM Application a " +
            "LEFT JOIN FETCH a.jobPosting " +
            "LEFT JOIN FETCH a.candidate " +
            "LEFT JOIN FETCH a.resume " +
            "WHERE a.jobPosting.id = :jobPostingId")
    Page<Application> findByJobPostingId(@Param("jobPostingId") Long jobPostingId, Pageable pageable);

    @Query("SELECT a FROM Application a " +
            "LEFT JOIN FETCH a.jobPosting " +
            "LEFT JOIN FETCH a.candidate " +
            "LEFT JOIN FETCH a.resume " +
            "WHERE a.candidate.id = :candidateId " +
            "ORDER BY a.appliedAt DESC")
    List<Application> findByCandidateId(@Param("candidateId") Long candidateId);

    @Query("SELECT a FROM Application a " +
            "LEFT JOIN FETCH a.jobPosting " +
            "LEFT JOIN FETCH a.candidate " +
            "LEFT JOIN FETCH a.resume " +
            "WHERE a.jobPosting.id = :jobPostingId AND a.status = :status")
    List<Application> findByJobPostingIdAndStatus(@Param("jobPostingId") Long jobPostingId,
                                                  @Param("status") ApplicationStatus status);

    boolean existsByJobPostingIdAndCandidateId(Long jobPostingId, Long candidateId);

    long countByJobPostingId(Long jobPostingId);
}