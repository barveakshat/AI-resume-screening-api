package com.resumescreening.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumescreening.api.exception.ResourceNotFoundException;
import com.resumescreening.api.model.dto.ParsedResumeData;
import com.resumescreening.api.model.dto.response.ResumeResponse;
import com.resumescreening.api.model.entity.Resume;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    private final ObjectMapper objectMapper;

    // Upload and parse resume - WITH parsed data in response
    @Transactional
    @CacheEvict(value = "userResumes", key = "#userId")
    public ResumeResponse uploadResume(Long userId, MultipartFile file) throws JsonProcessingException {
        User user = userService.getUserById(userId);

        // Upload file and extract text
        FileStorageService.FileUploadResult uploadResult = fileStorageService.storeResume(file);

        // Try to parse resume with AI - but don't fail if parsing fails
        String parsedDataJson = null;
        try {
            ParsedResumeData parsedData = resumeParserService.parseResume(uploadResult.getExtractedText());
            parsedDataJson = objectMapper.writeValueAsString(parsedData);
            log.info("Resume parsing successful for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to parse resume with AI for user {}: {}", userId, e.getMessage(), e);
            // Continue without parsed data - file is still uploaded
            log.warn("Resume will be saved without parsed data");
        }

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setFileName(uploadResult.getFileName());
        resume.setFilePath(uploadResult.getFileUrl());
        resume.setContentType(uploadResult.getFileType());
        resume.setFileSize(uploadResult.getFileSize());
        resume.setExtractedText(uploadResult.getExtractedText());
        resume.setParsedData(parsedDataJson);  // Will be null if parsing failed
        resume = resumeRepository.save(resume);

        log.info("Resume uploaded: {} for user {}", resume.getId(), userId);

        // Return WITH parsed data - user wants to see what was extracted
        return toResumeResponseWithParsedData(resume);
    }

    // Get resume by ID - WITH parsed data for detail view (not cached)
    @Transactional(readOnly = true)
    public ResumeResponse getResumeById(Long resumeId) {
        Resume resume = resumeRepository.findByIdWithUser(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));

        return toResumeResponseWithParsedData(resume);
    }

    // Internal method to get entity
    @Transactional(readOnly = true)
    public Resume getResumeEntityById(Long resumeId) {
        return resumeRepository.findByIdWithUser(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with id: " + resumeId));
    }

    // Get all resumes - WITHOUT parsed data (cached for performance)
    @Cacheable(value = "userResumes", key = "#userId")
    @Transactional(readOnly = true)
    public List<ResumeResponse> getResumesByUser(Long userId) {
        List<Resume> resumes = resumeRepository.findByUserId(userId);
        return resumes.stream()
                .map(this::toResumeResponseWithoutParsedData)
                .toList();
    }

    // Get parsed data from resume (not cached - fetch when needed)
    @Transactional(readOnly = true)
    public ParsedResumeData getParsedData(Long resumeId, Long userId) {
        Resume resume = getResumeEntityById(resumeId);
        validateOwnership(resume, userId);

        if (resume.getParsedData() == null || resume.getParsedData().trim().isEmpty()) {
            throw new IllegalStateException("Resume has not been parsed yet");
        }
        try {
            return objectMapper.readValue(resume.getParsedData(), ParsedResumeData.class);
        } catch (Exception e) {
            log.error("Failed to parse resume data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse resume data", e);
        }
    }

    // Delete resume
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "resumes", key = "#resumeId"),
            @CacheEvict(value = "userResumes", key = "#userId")
    })
    public void deleteResume(Long resumeId, Long userId) {
        Resume resume = getResumeEntityById(resumeId);
        validateOwnership(resume, userId);

        // Delete from S3
        try {
            fileStorageService.deleteFile(resume.getFilePath());
        } catch (Exception e) {
            log.warn("Failed to delete file from S3: {}", resume.getFilePath(), e);
        }

        resumeRepository.delete(resume);
        log.info("Resume deleted: {}", resumeId);
    }

    // Count resumes for user
    public long countResumesForUser(Long userId) {
        return resumeRepository.countByUserId(userId);
    }

    // Validate resume ownership
    private void validateOwnership(Resume resume, Long userId) {
        if (resume.getUser() == null || !resume.getUser().getId().equals(userId)) {
            throw new SecurityException("You don't have permission to access this resume");
        }
    }

    // Helper - WITH parsed data (for upload and detail view)
    private ResumeResponse toResumeResponseWithParsedData(Resume resume) {
        Long userId = null;
        if (resume.getUser() != null) {
            userId = resume.getUser().getId();
        }

        ParsedResumeData parsedData = null;
        String parsedDataString = resume.getParsedData();

        if (parsedDataString != null && !parsedDataString.trim().isEmpty()) {
            try {
                // Directly deserialize the JSON string
                parsedData = objectMapper.readValue(parsedDataString, ParsedResumeData.class);
                log.debug("Successfully parsed resume data for resume {}", resume.getId());
            } catch (Exception e) {
                log.error("Failed to deserialize resume data for resume {}: {}", resume.getId(), e.getMessage());
            }
        } else {
            log.debug("Resume {} has no parsed data", resume.getId());
        }

        return ResumeResponse.builder()
                .id(resume.getId())
                .userId(userId)
                .fileName(resume.getFileName())
                .filePath(resume.getFilePath())
                .fileType(resume.getContentType())
                .fileSize(resume.getFileSize())
                .uploadDate(resume.getUploadedAt())
                .parsedData(parsedData)
                .build();
    }

    // Helper - WITHOUT parsed data (for lists and caching)
    private ResumeResponse toResumeResponseWithoutParsedData(Resume resume) {
        Long userId = null;
        if (resume.getUser() != null) {
            userId = resume.getUser().getId();
        }

        return ResumeResponse.builder()
                .id(resume.getId())
                .userId(userId)
                .fileName(resume.getFileName())
                .filePath(resume.getFilePath())
                .fileType(resume.getContentType())
                .fileSize(resume.getFileSize())
                .uploadDate(resume.getUploadedAt())
                .parsedData(null)
                .build();
    }
}