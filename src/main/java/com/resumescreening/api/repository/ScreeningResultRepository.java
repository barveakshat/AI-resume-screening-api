package com.resumescreening.api.repository;

import com.resumescreening.api.model.entity.ScreeningResult;
import com.resumescreening.api.model.enums.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreeningResultRepository extends JpaRepository<ScreeningResult, Long> {

    /**
     * Find all screening results for a specific job posting.
     * Uses JOIN through Application entity.
     */
    @Query("SELECT sr FROM ScreeningResult sr WHERE sr.application.jobPosting.id = :jobId")
    List<ScreeningResult> findByApplicationJobPostingId(@Param("jobId") Long jobId);

    /**
     * Find screening result by application ID.
     */
    Optional<ScreeningResult> findByApplicationId(Long applicationId);

    /**
     * ✅ FIXED: Find screening results by job posting and recommendation level.
     * Must use @Query because we're navigating through relationships.
     */
    @Query("SELECT sr FROM ScreeningResult sr " +
            "WHERE sr.application.jobPosting.id = :jobId " +
            "AND sr.recommendation = :recommendation " +
            "ORDER BY sr.matchScore DESC")
    List<ScreeningResult> findByJobPostingIdAndRecommendation(
            @Param("jobId") Long jobId,
            @Param("recommendation") Recommendation recommendation
    );

    /**
     * Check if an application already has a screening result.
     * Prevents duplicate screening.
     */
    boolean existsByApplicationId(Long applicationId);

    /**
     * Count screening results for a job posting.
     */
    @Query("SELECT COUNT(sr) FROM ScreeningResult sr WHERE sr.application.jobPosting.id = :jobId")
    long countByJobPostingId(@Param("jobId") Long jobId);

    /**
     * Find top N screening results by match score for a job.
     * Useful for finding best candidates.
     */
    @Query("SELECT sr FROM ScreeningResult sr " +
            "WHERE sr.application.jobPosting.id = :jobId " +
            "ORDER BY sr.matchScore DESC")
    List<ScreeningResult> findTopCandidatesByJobPostingId(@Param("jobId") Long jobId);

    /**
     * ✅ NEW: Get screening results with match score above threshold
     */
    @Query("SELECT sr FROM ScreeningResult sr " +
            "WHERE sr.application.jobPosting.id = :jobId " +
            "AND sr.matchScore >= :minScore " +
            "ORDER BY sr.matchScore DESC")
    List<ScreeningResult> findByJobPostingIdAndMinScore(
            @Param("jobId") Long jobId,
            @Param("minScore") Integer minScore
    );

    /**
     * ✅ NEW: Get statistics for a job posting
     */
    @Query("SELECT " +
            "COUNT(sr), " +
            "AVG(sr.matchScore), " +
            "MAX(sr.matchScore), " +
            "MIN(sr.matchScore) " +
            "FROM ScreeningResult sr " +
            "WHERE sr.application.jobPosting.id = :jobId")
    Object[] getStatisticsByJobPostingId(@Param("jobId") Long jobId);

    /**
     * ✅ NEW: Find screening results for a candidate across all jobs
     */
    @Query("SELECT sr FROM ScreeningResult sr " +
            "WHERE sr.application.candidate.id = :candidateId " +
            "ORDER BY sr.createdAt DESC")
    List<ScreeningResult> findByCandidateId(@Param("candidateId") Long candidateId);

    /**
     * ✅ NEW: Find screening results by recruiter (job owner)
     */
    @Query("SELECT sr FROM ScreeningResult sr " +
            "WHERE sr.application.jobPosting.user.id = :recruiterId " +
            "ORDER BY sr.createdAt DESC")
    List<ScreeningResult> findByRecruiterId(@Param("recruiterId") Long recruiterId);
}