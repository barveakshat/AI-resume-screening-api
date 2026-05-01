package com.resumescreening.api.security.ratelimit;

import java.time.Duration;

public record RateLimitRule(String name, long limit, Duration window) {
}
