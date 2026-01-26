package com.resumescreening.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.resumescreening.api.model.dto.response.ApiResponse;
import com.resumescreening.api.model.dto.response.ResumeResponse;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.service.ResumeService;
import com.resumescreening.api.service.UserService;
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
    private final UserService userService;

    // Upload resume
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ResumeResponse>> uploadResume(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) throws JsonProcessingException {
        User user = getAuthenticatedUser(authentication);

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
        User user = getAuthenticatedUser(authentication);

        List<ResumeResponse> responses = resumeService.getResumesByUser(user.getId());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // Get resume by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResumeResponse>> getResumeById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);
        ResumeResponse response = resumeService.getResumeById(id);
        // Validate ownership - with null check
        if (response.getUserId() != null && !response.getUserId().equals(user.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You don't have permission to access this resume"));
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    // Delete resume
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteResume(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = getAuthenticatedUser(authentication);

        resumeService.deleteResume(id, user.getId());

        return ResponseEntity.ok(ApiResponse.success("Resume deleted successfully", null));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}