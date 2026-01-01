package com.resumescreening.api.model.entity;

import com.resumescreening.api.model.enums.Role;
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
 * User entity - represents both recruiters and candidates.
 *
 * Key fields:
 * - role: Determines what features user can access
 * - companyName/designation: Only for recruiters
 * - isEmailVerified: For future email verification feature
 *
 * Relationships:
 * - One user can have many job postings (if recruiter)
 * - One user can have many resumes (both roles can upload)
 */
@Setter
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;  // Will be BCrypt encrypted

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)  // Store as "RECRUITER" or "CANDIDATE" in DB
    @Column(nullable = false, length = 50)
    private Role role;

    @Column(name = "company_name", length = 255)
    private String companyName;  // Only for recruiters

    @Column(length = 255)
    private String designation;  // Only for recruiters

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "is_email_verified")
    private Boolean isEmailVerified = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp  // Automatically set on creation
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp  // Automatically updated on every save
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships

    /**
     * One user can create many job postings (if recruiter).
     * mappedBy = "user" means JobPosting entity has a 'user' field.
     * cascade = ALL means if we delete a user, delete their jobs too.
     * orphanRemoval = true means if job is removed from list, delete it.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobPosting> jobPostings = new ArrayList<>();

    /**
     * One user can upload many resumes.
     * Both recruiters and candidates can upload resumes.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Resume> resumes = new ArrayList<>();

    // ============================================
    // HELPER METHODS (Optional but useful)
    // ============================================
    /**
     * Helper method to add a job posting.
     *
     * BEST PRACTICE: Maintain bidirectional relationship
     * - Add job to user's list
     * - Set user on job (so foreign key is saved)
     *
     * WHY THIS MATTERS:
     * - Without this: jobPosting.setUser(user) must be called separately
     * - With this: user.addJobPosting(job) does both!
     */
    public void addJobPosting(JobPosting jobPosting) {
        jobPostings.add(jobPosting);
        jobPosting.setUser(this);
    }

    public void removeJobPosting(JobPosting jobPosting) {
        jobPostings.remove(jobPosting);
        jobPosting.setUser(null);
    }

    public void addResume(Resume resume) {
        resumes.add(resume);
        resume.setUser(this);
    }

    public void removeResume(Resume resume) {
        resumes.remove(resume);
        resume.setUser(null);
    }
}