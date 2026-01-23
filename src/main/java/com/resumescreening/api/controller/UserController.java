package com.resumescreening.api.controller;

import com.resumescreening.api.model.dto.request.UpdateProfileRequest;
import com.resumescreening.api.model.dto.request.ChangePasswordRequest;
import com.resumescreening.api.model.dto.response.ApiResponse;
import com.resumescreening.api.model.dto.response.UserResponse;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.service.UserService;
import com.resumescreening.api.util.DtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = getCurrentUserId();
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
            @Valid @RequestBody ChangePasswordRequest request) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("New password and confirmation do not match"));
        }

        Long userId = getCurrentUserId();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }


    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> deactivateAccount() {
        Long userId = getCurrentUserId();
        userService.deactivateAccount(userId);
        return ResponseEntity.ok(ApiResponse.success("Account deactivated", null));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        User user = userService.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}
