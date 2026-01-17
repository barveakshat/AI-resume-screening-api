package com.resumescreening.api.model.entity;

import com.resumescreening.api.converter.StringListConverter;
import com.resumescreening.api.model.enums.EmploymentType;
import com.resumescreening.api.model.enums.ExperienceLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JobPosting entity - represents jobs created by recruiters.
 *
 * Purpose:
 * - Recruiters create job postings with requirements
 * - These are used to screen candidates' resumes
 * - AI compares resume against job requirements
 *
 * Key fields:
 * - requiredSkills: Array of skills needed (Spring Boot, AWS, etc.)
 * - experienceLevel: Entry/Mid/Senior/Lead
 * - isActive: Whether job is still open for applications
 *
 * Relationships:
 * - ManyToOne with User (many jobs belong to one recruiter)
 * - OneToMany with ScreeningResult (one job can have many screening results)
 */
@Setter
@Getter
@Entity
@Table(name = "job_postings")
@NoArgsConstructor
@AllArgsConstructor
public class JobPosting {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ManyToOne: Many jobs can belong to one user
     * fetch = LAZY: Don't load user data unless explicitly accessed (performance)
     * JoinColumn: Creates foreign key column 'user_id' in job_postings table
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;  // e.g., "Senior Java Developer"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;  // Full job description


    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> requiredSkills;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", length = 50)
    private ExperienceLevel experienceLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 50)
    private EmploymentType employmentType;

    @Column(length = 255)
    private String location;  // e.g., "Bangalore, India"

    @Column(name = "salary_range", length = 100)
    private String salaryRange;  // e.g., "₹12-18 LPA"

    @Column(name = "is_active")
    private Boolean isActive = true;  // Can be deactivated when position is filled

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ✅ CHANGED: Relationship to Applications instead of ScreeningResults
    @OneToMany(mappedBy = "jobPosting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Application> applications = new ArrayList<>();

    public JobPosting(User user, String title, String description) {
        this.user = user;
        this.title = title;
        this.description = description;
    }

}