package com.resumescreening.api.service;

import com.resumescreening.api.exception.ResourceNotFoundException;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.Role;
import com.resumescreening.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Register new user
    @Transactional
    public User registerUser(String email, String password, String fullName,
                             Role role, String companyName, String designation) {

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        // Validate role-specific fields
        if (role == Role.RECRUITER && (companyName == null || companyName.trim().isEmpty())) {
            throw new IllegalArgumentException("Company name is required for recruiters");
        }

        // Create user entity
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        user.setCompanyName(companyName);
        user.setDesignation(designation);
        user.setIsEmailVerified(false);
        user.setIsActive(true);

        // Save to database
        user = userRepository.save(user);

        log.info("User registered successfully: {} ({})", email, role);
        return user;
    }

    // Find user by email
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Get user by ID
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    // Get active user by email
    public Optional<User> findActiveUserByEmail(String email) {
        return userRepository.findActiveUserByEmail(email);
    }

    // Update user profile
    @Transactional
    public User updateProfile(Long userId, String fullName, String phoneNumber,
                              String companyName, String designation) {
        User user = getUserById(userId);

        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName);
        }
        if (phoneNumber != null) {
            user.setPhoneNumber(phoneNumber);
        }
        if (user.getRole() == Role.RECRUITER) {
            if (companyName != null) {
                user.setCompanyName(companyName);
            }
            if (designation != null) {
                user.setDesignation(designation);
            }
        }

        log.info("Profile updated for user: {}", userId);
        return userRepository.save(user);
    }

    // Change password
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }

    // Deactivate account (soft delete)
    @Transactional
    public void deactivateAccount(Long userId) {
        User user = getUserById(userId);
        user.setIsActive(false);
        userRepository.save(user);

        log.info("Account deactivated for user: {}", userId);
    }

    // Get all users by role
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRoleAndIsActiveTrue(role);
    }

    // Count users by role
    public long countUsersByRole(Role role) {
        return userRepository.countByRole(role);
    }
}