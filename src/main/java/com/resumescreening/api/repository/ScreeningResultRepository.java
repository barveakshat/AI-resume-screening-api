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

    // Fix: Add JOIN FETCH for collections
    @Query("SELECT sr FROM ScreeningResult sr " +
            "LEFT JOIN FETCH sr.application " +
            "LEFT JOIN FETCH sr.jobPosting " +
            "WHERE sr.application.jobPosting.id = :jobId")
    List<ScreeningResult> findByApplicationJobPostingId(@Param("jobId") Long jobId);

    @Query("SELECT sr FROM ScreeningResult sr " +
            "LEFT JOIN FETCH sr.application " +
            "LEFT JOIN FETCH sr.jobPosting " +
            "WHERE sr.application.id = :applicationId")
    Optional<ScreeningResult> findByApplicationId(@Param("applicationId") Long applicationId);

    @Query("SELECT sr FROM ScreeningResult sr " +
            "LEFT JOIN FETCH sr.application " +
            "LEFT JOIN FETCH sr.jobPosting " +
            "WHERE sr.application.jobPosting.id = :jobId " +
            "AND sr.recommendation = :recommendation " +
            "ORDER BY sr.matchScore DESC")
    List<ScreeningResult> findByJobPostingIdAndRecommendation(
            @Param("jobId") Long jobId,
            @Param("recommendation") Recommendation recommendation
    );

    boolean existsByApplicationId(Long applicationId);

    @Query("SELECT COUNT(sr) FROM ScreeningResult sr WHERE sr.application.jobPosting.id = :jobId")
    long countByJobPostingId(@Param("jobId") Long jobId);

    @Query("SELECT sr FROM ScreeningResult sr " +
            "LEFT JOIN FETCH sr.application " +
            "LEFT JOIN FETCH sr.jobPosting " +
            "WHERE sr.application.jobPosting.id = :jobId " +
            "ORDER BY sr.matchScore DESC")
    List<ScreeningResult> findTopCandidatesByJobPostingId(@Param("jobId") Long jobId);

    @Query("SELECT sr FROM ScreeningResult sr " +
            "LEFT JOIN FETCH sr.application " +
            "LEFT JOIN FETCH sr.jobPosting " +
            "WHERE sr.application.jobPosting.id = :jobId " +
            "AND sr.matchScore >= :minScore " +
            "ORDER BY sr.matchScore DESC")
    List<ScreeningResult> findByJobPostingIdAndMinScore(
            @Param("jobId") Long jobId,
            @Param("minScore") Integer minScore
    );

    @Query("SELECT " +
            "COUNT(sr), " +
            "AVG(sr.matchScore), " +
            "MAX(sr.matchScore), " +
            "MIN(sr.matchScore) " +
            "FROM ScreeningResult sr " +
            "WHERE sr.application.jobPosting.id = :jobId")
    Object[] getStatisticsByJobPostingId(@Param("jobId") Long jobId);

    @Query("SELECT sr FROM ScreeningResult sr " +
            "LEFT JOIN FETCH sr.application " +
            "LEFT JOIN FETCH sr.jobPosting " +
            "WHERE sr.application.candidate.id = :candidateId " +
            "ORDER BY sr.createdAt DESC")
    List<ScreeningResult> findByCandidateId(@Param("candidateId") Long candidateId);

    @Query("SELECT sr FROM ScreeningResult sr " +
            "LEFT JOIN FETCH sr.application " +
            "LEFT JOIN FETCH sr.jobPosting " +
            "WHERE sr.application.jobPosting.user.id = :recruiterId " +
            "ORDER BY sr.createdAt DESC")
    List<ScreeningResult> findByRecruiterId(@Param("recruiterId") Long recruiterId);
}