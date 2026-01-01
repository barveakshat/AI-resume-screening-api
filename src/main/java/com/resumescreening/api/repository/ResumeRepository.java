package com.resumescreening.api.repository;

import com.resumescreening.api.model.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    // Find all resumes uploaded by a user
    List<Resume> findByUserId(Long userId);

    // Find by file name
    Optional<Resume> findByFileName(String fileName);

    // Find resumes uploaded in date range
    List<Resume> findByUploadDateBetween(LocalDateTime start, LocalDateTime end);

    // Find resumes with parsed data (not null)
    @Query("SELECT r FROM Resume r WHERE r.parsedData IS NOT NULL")
    List<Resume> findAllWithParsedData();

    // Custom query - search in parsed data (PostgreSQL JSONB)
    @Query(value = "SELECT * FROM resumes WHERE parsed_data->>'email' = :email",
            nativeQuery = true)
    List<Resume> findByParsedEmail(@Param("email") String email);

    // Count resumes by user
    long countByUserId(Long userId);
}