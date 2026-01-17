package com.resumescreening.api.controller;

import com.resumescreening.api.model.dto.response.ApiResponse;
import com.resumescreening.api.model.dto.response.ResumeResponse;
import com.resumescreening.api.model.entity.Resume;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.service.ResumeService;
import com.resumescreening.api.service.UserService;
import com.resumescreening.api.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;
    private final UserService userService;

    // Upload resume
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ResumeResponse>> uploadResume(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resume resume = resumeService.uploadResume(user.getId(), file);

        ResumeResponse response = DtoMapper.toResumeResponse(resume);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resume uploaded and parsed successfully", response));
    }

    // Get all resumes for current user
    @GetMapping
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> getMyResumes(
            Authentication authentication
    ) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Resume> resumes = resumeService.getResumesByUser(user.getId());

        List<ResumeResponse> responses = resumes.stream()
                .map(DtoMapper::toResumeResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success(responses)
        );
    }

    // Get resume by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResumeResponse>> getResumeById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Resume resume = resumeService.getResumeById(id);

        // Validate ownership (service layer also validates)
        if (!resume.getUser().getId().equals(user.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You don't have permission to access this resume"));
        }

        ResumeResponse response = DtoMapper.toResumeResponse(resume);

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    // Delete resume
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteResume(
            @PathVariable Long id,
            Authentication authentication
    ) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        resumeService.deleteResume(id, user.getId());

        return ResponseEntity.ok(
                ApiResponse.success("Resume deleted successfully", null)
        );
    }
}