package com.resumescreening.api.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Resume entity - represents uploaded resume files.
 *
 * Purpose:
 * - Stores metadata about uploaded resume files
 * - Actual file is stored in AWS S3
 * - parsedData contains extracted information (JSON format)
 *
 * Workflow:
 * 1. User uploads PDF/DOCX file
 * 2. File uploaded to S3 → get URL → save in filePath
 * 3. Extract text from file
 * 4. Send to OpenAI for parsing
 * 5. Store parsed data in parsedData field
 *
 * Relationships:
 * - ManyToOne with User (many resumes belong to one user)
 * - OneToMany with ScreeningResult (one resume can be screened against many jobs)
 */
@Setter
@Getter
@Entity
@Table(name = "resumes")
public class Resume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ManyToOne: Many resumes can belong to one user
     * Both recruiters and candidates can upload resumes
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;  // Original filename: "Akshat_Barve_Resume.pdf"

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;  // S3 URL: "https://s3.amazonaws.com/.../resume.pdf"

    @Column(name = "file_type", length = 50)
    private String fileType;  // "application/pdf" or "application/vnd.openxmlformats..."

    @Column(name = "file_size")
    private Long fileSize;  // Size in bytes

    /**
     * PostgreSQL JSONB column - stores structured data.
     * Example content:
     * {
     *   "fullName": "Akshat Barve",
     *   "email": "barveakshat091@gmail.com",
     *   "skills": ["Java", "Spring Boot", "AWS"],
     *   "experience": [...],
     *   "education": [...],
     *   "totalExperienceYears": 0
     * }
     *
     * JSONB is better than JSON because:
     * - Faster queries
     * - Can index specific fields
     * - Binary storage (compressed)
     */
    @Column(name = "parsed_data", columnDefinition = "JSONB")
    private String parsedData;  // We'll store as String, convert to/from JSON as needed

    @CreationTimestamp
    @Column(name = "upload_date", nullable = false, updatable = false)
    private LocalDateTime uploadDate;

    /**
     * OneToMany: One resume can be screened against many jobs
     * Example: Same resume screened for "Java Dev" and "Full Stack Dev" jobs
     */
    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScreeningResult> screeningResults = new ArrayList<>();

    // Constructors
    public Resume() {
    }

    public Resume(User user, String fileName, String filePath, String fileType, Long fileSize) {
        this.user = user;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }

}