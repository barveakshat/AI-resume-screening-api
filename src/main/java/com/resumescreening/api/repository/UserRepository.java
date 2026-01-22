package com.resumescreening.api.repository;

import com.resumescreening.api.model.entity.User;
import com.resumescreening.api.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Derived query - Spring generates SQL automatically
    Optional<User> findByEmail(String email);

    // Check existence
    boolean existsByEmail(String email);

    // Find active users by role
    List<User> findByRoleAndIsActiveTrue(Role role);

    // Custom query using JPQL
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isActive = true")
    Optional<User> findActiveUserByEmail(@Param("email") String email);

    // Count by role
    long countByRole(Role role);
}