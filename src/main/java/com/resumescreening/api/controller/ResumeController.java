package com.resumescreening.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.resumescreening.api.model.dto.response.ApiResponse;
import com.resumescreening.api.model.dto.response.ResumeResponse;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.service.CurrentUserService;
import com.resumescreening.api.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
public class ResumeController {

    private final ResumeService resumeService;
    private final CurrentUserService currentUserService;

    // Upload resume
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ResumeResponse>> uploadResume(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) throws JsonProcessingException {
        User user = currentUserService.getCurrentUser(authentication);

        ResumeResponse response = resumeService.uploadResume(user.getId(), file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resume uploaded and parsed successfully", response));
    }

    // Get all resumes for current user
    @GetMapping("/my-resumes")
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> getMyResumes(
            Authentication authentication
    ) {
        User user = currentUserService.getCurrentUser(authentication);

        List<ResumeResponse> responses = resumeService.getResumesByUser(user.getId());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // Get resume by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResumeResponse>> getResumeById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = currentUserService.getCurrentUser(authentication);
        ResumeResponse response = resumeService.getResumeById(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    // Delete resume
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteResume(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = currentUserService.getCurrentUser(authentication);

        resumeService.deleteResume(id, user.getId());

        return ResponseEntity.ok(ApiResponse.success("Resume deleted successfully", null));
    }
}
