package com.resumescreening.api.model.entity;

import com.resumescreening.api.converter.JsonConverter;
import com.resumescreening.api.model.enums.Recommendation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ScreeningResult entity - stores AI analysis of resume vs job match.
 *
 * Purpose:
 * - After screening a resume against a job, AI generates scores and analysis
 * - This entity stores all that data
 * - Recruiters use this to decide which candidates to interview
 *
 * Scoring:
 * - Overall Score: 0-100 (weighted average of all factors)
 * - Skill Match: How many required skills candidate has
 * - Experience Match: Does experience level match?
 * - Education Match: Does education meet requirements?
 *
 * Relationships:
 * - ManyToOne with JobPosting (many results for one job)
 * - ManyToOne with Resume (many results for one resume)
 * - Unique constraint: One job + one resume = one result only
 */

@Setter
@Getter
@Entity
@Table(
        name = "screening_results",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_job_resume",
                        columnNames = {"job_posting_id", "resume_id"}
                )
        }
)
@AllArgsConstructor
@NoArgsConstructor
public class ScreeningResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ManyToOne: Many screening results for one job
     * Example: Job "Java Developer" screened against 10 resumes = 10 results
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    /**
     * ManyToOne: Many screening results for one resume
     * Example: Resume screened against 5 jobs = 5 results
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    // Scores (all 0-100)

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;  // BigDecimal for precise decimal storage

    @Column(name = "skill_match_score", precision = 5, scale = 2)
    private BigDecimal skillMatchScore;

    @Column(name = "experience_match_score", precision = 5, scale = 2)
    private BigDecimal experienceMatchScore;

    @Column(name = "education_match_score", precision = 5, scale = 2)
    private BigDecimal educationMatchScore;

    // Detailed Analysis (stored as JSON text)

    /**
     * Matched skills as JSON array
     * Example: ["Spring Boot", "MySQL", "AWS"]
     */
    @Convert(converter = JsonConverter.class)
    @Column(columnDefinition = "JSONB")
    private Object matchedSkills;

    @Convert(converter = JsonConverter.class)
    @Column(columnDefinition = "JSONB")
    private Object missingSkills;

    /**
     * AI-generated strengths
     * Example: "Strong foundational knowledge of Spring Boot ecosystem..."
     */
    @Column(columnDefinition = "TEXT")
    private String strengths;

    /**
     * AI-generated weaknesses
     * Example: "No professional work experience. Limited microservices exposure..."
     */
    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    /**
     * AI-generated summary (2-3 sentences)
     * Example: "Akshat shows promise as an entry-level hire with solid..."
     */
    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    /**
     * Final recommendation based on overall score
     * - STRONG_FIT: >= 80
     * - GOOD_FIT: >= 60
     * - MODERATE_FIT: >= 40
     * - POOR_FIT: < 40
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Recommendation recommendation;

    /**
     * How long screening took (in milliseconds)
     * Used for performance monitoring
     * Example: 4200 = 4.2 seconds
     */
    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @CreationTimestamp
    @Column(name = "screened_at", nullable = false, updatable = false)
    private LocalDateTime screenedAt;

    public ScreeningResult(JobPosting jobPosting, Resume resume) {
        this.jobPosting = jobPosting;
        this.resume = resume;
    }

}