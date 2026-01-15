package com.resumescreening.api.model.dto.response;

import com.resumescreening.api.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private Long expiresIn;

    // User info
    private Long userId;
    private String email;
    private String fullName;
    private Role role;
    private String companyName;
    private Boolean isEmailVerified;
}