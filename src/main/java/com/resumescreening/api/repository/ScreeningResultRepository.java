package com.resumescreening.api.repository;

import com.resumescreening.api.model.entity.ScreeningResult;
import com.resumescreening.api.model.enums.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScreeningResultRepository extends JpaRepository<ScreeningResult, Long> {

    // Find all results for a job
    List<ScreeningResult> findByJobPostingId(Long jobPostingId);

    // Find all results for a resume
    List<ScreeningResult> findByResumeId(Long resumeId);

    // Find by job and resume (unique constraint)
    Optional<ScreeningResult> findByJobPostingIdAndResumeId(Long jobPostingId, Long resumeId);

    // Find by recommendation level
    List<ScreeningResult> findByJobPostingIdAndRecommendation(
            Long jobPostingId,
            Recommendation recommendation
    );

    // Find top candidates (ordered by score)
    List<ScreeningResult> findByJobPostingIdOrderByOverallScoreDesc(Long jobPostingId);

    // Find candidates above score threshold
    List<ScreeningResult> findByJobPostingIdAndOverallScoreGreaterThanEqual(
            Long jobPostingId,
            BigDecimal minScore
    );

    // Check if screening exists
    boolean existsByJobPostingIdAndResumeId(Long jobPostingId, Long resumeId);

    // Analytics: Average score for a job
    @Query("SELECT AVG(s.overallScore) FROM ScreeningResult s WHERE s.jobPosting.id = :jobId")
    BigDecimal getAverageScoreForJob(@Param("jobId") Long jobId);

    // Count results by recommendation
    long countByJobPostingIdAndRecommendation(Long jobPostingId, Recommendation recommendation);
}