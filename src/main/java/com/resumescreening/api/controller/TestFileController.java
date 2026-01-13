package com.resumescreening.api.controller;

import com.resumescreening.api.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestFileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload-resume")
    public ResponseEntity<Map<String, Object>> uploadResume(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            // Upload and extract text
            FileStorageService.FileUploadResult result = fileStorageService.storeResume(file);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("fileName", result.getFileName());
            response.put("fileUrl", result.getFileUrl());
            response.put("fileSize", result.getFileSize());
            response.put("extractedTextLength", result.getExtractedText().length());
            response.put("extractedTextPreview",
                    result.getExtractedText().substring(0, Math.min(200, result.getExtractedText().length())));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "File service is ready");
        return ResponseEntity.ok(response);
    }
}