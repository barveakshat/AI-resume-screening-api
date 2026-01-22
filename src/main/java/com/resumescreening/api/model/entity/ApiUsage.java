package com.resumescreening.api.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * ApiUsage entity - tracks API usage for rate limiting.
 * Purpose:
 * - Prevent abuse of API (too many requests)
 * - Track usage per user per endpoint
 * - Reset counters periodically (e.g., every hour)
 * Example:
 * - User makes 50 screening requests in 1 hour
 * - request_count = 50
 * - If limit is 100, they can make 50 more
 * - After 1 hour, counter resets to 0
 * Relationships:
 * - ManyToOne with User (track each user's usage)
 * - Unique constraint: One user + one endpoint = one record
 */
@Setter
@Getter
@Entity
@Table(
        name = "api_usage",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_user_endpoint",
                        columnNames = {"user_id", "endpoint"}
                )
        }
)
@AllArgsConstructor
@NoArgsConstructor
public class ApiUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String endpoint;  // e.g., "/api/v1/screening/analyze"

    @Column(name = "request_count")
    private Integer requestCount = 0;  // Number of requests made

    @CreationTimestamp
    @Column(name = "last_reset", nullable = false)
    private LocalDateTime lastReset;  // When counter was last reset

}