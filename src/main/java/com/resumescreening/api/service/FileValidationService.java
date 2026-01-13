package com.resumescreening.api.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
public class FileValidationService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "docx", "doc");
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
    );

    // Validate file
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        validateFileSize(file);
        validateFileExtension(file);
        validateContentType(file);
    }

    // Validate file size
    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds maximum limit of %d MB",
                            MAX_FILE_SIZE / (1024 * 1024))
            );
        }
    }

    // Validate file extension
    private void validateFileExtension(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    String.format("File extension '%s' not allowed. Allowed: %s",
                            extension, ALLOWED_EXTENSIONS)
            );
        }
    }

    // Validate content type
    private void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    String.format("Content type '%s' not allowed", contentType)
            );
        }
    }

    // Check if file is PDF
    public boolean isPdf(MultipartFile file) {
        return "application/pdf".equals(file.getContentType());
    }

    // Check if file is DOCX
    public boolean isDocx(MultipartFile file) {
        String contentType = file.getContentType();
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                .equals(contentType);
    }
}