package com.resumescreening.api.service;

import com.resumescreening.api.exception.ResourceNotFoundException;
import com.resumescreening.api.model.dto.ParsedResumeData;
import com.resumescreening.api.model.entity.Resume;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final ResumeParserService resumeParserService;


    // Upload and parse resume
    @Transactional
    public Resume uploadResume(Long userId, MultipartFile file) {
        User user = userService.getUserById(userId);

        // Upload file and extract text
        FileStorageService.FileUploadResult uploadResult = fileStorageService.storeResume(file);

        // Parse resume with AI
        ParsedResumeData parsedData = resumeParserService.parseResume(uploadResult.getExtractedText());

        // Create resume entity
        Resume resume = new Resume();
        resume.setUser(user);
        resume.setFileName(uploadResult.getFileName());
        resume.setFilePath(uploadResult.getFileUrl());
        resume.setFileType(uploadResult.getFileType());
        resume.setFileSize(uploadResult.getFileSize());
        resume.setParsedData(parsedData);

        resume = resumeRepository.save(resume);

        log.info("Resume uploaded and parsed: {} for user {}", resume.getId(), userId);
        return resume;
    }

    // Get resume by ID
    public Resume getResumeById(Long resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));
    }

    // Get all resumes for a user
    public List<Resume> getResumesByUser(Long userId) {
        return resumeRepository.findByUserId(userId);
    }

    // Get parsed data from resume
    public ParsedResumeData getParsedData(Long resumeId, Long userId) {
        Resume resume = getResumeById(resumeId);
        validateOwnership(resume, userId);

        if (resume.getParsedData() == null) {
            throw new IllegalStateException("Resume has not been parsed yet");
        }

        return (ParsedResumeData) resume.getParsedData();
    }

    // Delete resume
    @Transactional
    public void deleteResume(Long resumeId, Long userId) {
        Resume resume = getResumeById(resumeId);
        validateOwnership(resume, userId);

        // Delete from S3
        try {
            fileStorageService.deleteFile(resume.getFilePath());
        } catch (Exception e) {
            log.warn("Failed to delete file from S3: {}", resume.getFilePath(), e);
        }

        // Delete from database
        resumeRepository.delete(resume);

        log.info("Resume deleted: {}", resumeId);
    }

    // Count resumes for user
    public long countResumesForUser(Long userId) {
        return resumeRepository.countByUserId(userId);
    }

    // Validate resume ownership
    private void validateOwnership(Resume resume, Long userId) {
        if (!resume.getUser().getId().equals(userId)) {
            throw new SecurityException("You don't have permission to access this resume");
        }
    }
}