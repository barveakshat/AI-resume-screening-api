package com.resumescreening.api.model.dto.response;

import com.resumescreening.api.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private Role role;
    private String companyName;
    private String designation;
    private String phoneNumber;
    private Boolean isEmailVerified;
    private Boolean isActive;
    private LocalDateTime createdAt;
}