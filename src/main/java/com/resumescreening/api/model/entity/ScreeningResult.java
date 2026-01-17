package com.resumescreening.api.model.entity;

import com.resumescreening.api.model.enums.Recommendation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "screening_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreeningResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ CHANGED: Link to Application instead of Resume and JobPosting separately
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @Column(nullable = false)
    private Integer matchScore; // 0-100

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Recommendation recommendation;

    @Column(name = "skill_match_score")
    private Integer skillMatchScore; // 0-100

    @Column(name = "experience_match_score")
    private Integer experienceMatchScore; // 0-100

    @Column(name = "education_match_score")
    private Integer educationMatchScore;

    @ElementCollection
    @CollectionTable(name = "matched_skills", joinColumns = @JoinColumn(name = "screening_result_id"))
    @Column(name = "skill")
    private List<String> matchedSkills = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "missing_skills", joinColumns = @JoinColumn(name = "screening_result_id"))
    @Column(name = "skill")
    private List<String> missingSkills = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String weaknesses;

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
}