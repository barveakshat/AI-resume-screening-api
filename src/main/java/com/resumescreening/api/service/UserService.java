package com.resumescreening.api.service;

import com.resumescreening.api.exception.ResourceNotFoundException;
import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.Role;
import com.resumescreening.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional          // Register new user - evict any stale cache entries
    @CacheEvict(value = "users", key = "'email_' + #email")
    public User registerUser(String email, String password, String fullName,
                             Role role, String companyName, String designation) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }
        if (role == Role.RECRUITER && (companyName == null || companyName.trim().isEmpty())) {
            throw new IllegalArgumentException("Company name is required for recruiters");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        user.setCompanyName(companyName);
        user.setDesignation(designation);
        user.setIsEmailVerified(false);
        user.setIsActive(true);
        user = userRepository.save(user);

        log.info("User registered successfully: {} ({})", email, role);
        return user;
    }

    // Find user by email - cached
    @Cacheable(value = "users", key = "'email_' + #email", unless = "#result == null")
    public Optional<User> findByEmail(String email) {
        log.debug("Fetching user from database by email: {}", email);
        return userRepository.findByEmail(email);
    }
    // Get user by ID - cached (frequently called for auth checks)
    @Cacheable(value = "users", key = "'id_' + #userId")
    public User getUserById(Long userId) {
        log.debug("Fetching user from database by id: {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    // Update user profile - evict both id and email cache entries
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", key = "'id_' + #userId"),
            @CacheEvict(value = "users", key = "'email_' + #result.email")
    })
    public User updateProfile(Long userId, String fullName, String phoneNumber,
                              String companyName, String designation) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

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

    // Change password - evict cache
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "users", key = "'id_' + #userId"),
            @CacheEvict(value = "users")
    })
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }

    // Deactivate account - evict cache
    @Transactional
    @CacheEvict(value = "users", key = "'id_' + #userId")
    public void deactivateAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setIsActive(false);
        userRepository.save(user);

        log.info("Account deactivated for user: {}", userId);
    }
}
