package com.resumescreening.api.security.ratelimit;

public record RateLimitDecision(
        boolean allowed,
        long limit,
        long remaining,
        long retryAfterSeconds
) {
}
