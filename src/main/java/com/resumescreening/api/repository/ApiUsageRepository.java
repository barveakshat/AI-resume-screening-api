package com.resumescreening.api.repository;

import com.resumescreening.api.model.entity.ApiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ApiUsageRepository extends JpaRepository<ApiUsage, Long> {

    // Use user.id instead of userId (navigating the relationship)
    Optional<ApiUsage> findByUserIdAndEndpoint(Long userId, String endpoint);

    // Fixed: Use a.user.id to navigate the relationship
    @Modifying
    @Query("UPDATE ApiUsage a SET a.requestCount = a.requestCount + 1 WHERE a.user.id = :userId AND a.endpoint = :endpoint")
    int incrementRequestCount(@Param("userId") Long userId, @Param("endpoint") String endpoint);

    // Fixed: Use a.user.id
    @Modifying
    @Query("UPDATE ApiUsage a SET a.requestCount = 0, a.lastReset = :resetTime WHERE a.user.id = :userId AND a.endpoint = :endpoint")
    int resetUsage(@Param("userId") Long userId,
                   @Param("endpoint") String endpoint,
                   @Param("resetTime") LocalDateTime resetTime);

    void deleteByLastResetBefore(LocalDateTime cutoffTime);
}