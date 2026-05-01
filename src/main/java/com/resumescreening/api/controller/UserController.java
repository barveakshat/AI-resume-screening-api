package com.resumescreening.api.controller;

import com.resumescreening.api.model.dto.request.ChangePasswordRequest;
import com.resumescreening.api.model.dto.request.UpdateProfileRequest;
import com.resumescreening.api.model.dto.response.ApiResponse;
import com.resumescreening.api.model.dto.response.UserResponse;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.service.CurrentUserService;
import com.resumescreening.api.service.UserService;
import com.resumescreening.api.util.DtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserService currentUserService;

    @PutMapping("/updateprofile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        Long userId = currentUserService.getCurrentUserId(authentication);
        User user = userService.updateProfile(
                userId,
                request.getFullName(),
                request.getPhoneNumber(),
                request.getCompanyName(),
                request.getDesignation()
        );
        return ResponseEntity.ok(ApiResponse.success("Profile updated", DtoMapper.toUserResponse(user)));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        Long userId = currentUserService.getCurrentUserId(authentication);
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @DeleteMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount(Authentication authentication) {
        Long userId = currentUserService.getCurrentUserId(authentication);
        userService.deactivateAccount(userId);
        return ResponseEntity.ok(ApiResponse.success("Account deactivated", null));
    }
}
