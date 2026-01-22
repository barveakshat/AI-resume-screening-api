package com.resumescreening.api.service;

import com.resumescreening.api.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final S3Service s3Service;
    private final FileValidationService validationService;
    private final TextExtractionService textExtractionService;

    // Store resume file
    public FileUploadResult storeResume(MultipartFile file) {
        try {
            // Validate file & Upload to S3
            validationService.validateFile(file);
            String fileUrl = s3Service.uploadFile(file, "resumes");

            // Extract text & Return result
            String extractedText = textExtractionService.extractText(file);
            return FileUploadResult.builder()
                    .fileName(file.getOriginalFilename())
                    .fileUrl(fileUrl)
                    .fileSize(file.getSize())
                    .fileType(file.getContentType())
                    .extractedText(extractedText)
                    .build();

        } catch (IOException e) {
            log.error("Error storing resume: {}", e.getMessage());
            throw new FileStorageException("Failed to store resume file", e);
        }
    }

    // Delete file
    public void deleteFile(String fileUrl) {
        try {
            String fileKey = s3Service.extractFileKeyFromUrl(fileUrl);
            s3Service.deleteFile(fileKey);

        } catch (Exception e) {
            log.error("Error deleting file: {}", e.getMessage());
            throw new FileStorageException("Failed to delete file", e);
        }
    }

    // Inner class for upload result
    @lombok.Builder
    @lombok.Data
    public static class FileUploadResult {
        private String fileName;
        private String fileUrl;
        private Long fileSize;
        private String fileType;
        private String extractedText;
    }
}