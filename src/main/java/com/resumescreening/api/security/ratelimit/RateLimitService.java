package com.resumescreening.api.security.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final Clock clock = Clock.systemUTC();

    public RateLimitDecision checkLimit(String subjectType, String subject, RateLimitRule rule) {
        long windowSeconds = rule.window().toSeconds();
        long window = clock.instant().getEpochSecond() / windowSeconds;
        String key = "rate:%s:%s:%s:%d".formatted(subjectType, subject, rule.name(), window);

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, rule.window());
        }

        long used = count != null ? count : rule.limit() + 1;
        long remaining = Math.max(0, rule.limit() - used);
        long retryAfter = secondsUntilNextWindow(windowSeconds);

        return new RateLimitDecision(used <= rule.limit(), rule.limit(), remaining, retryAfter);
    }

    private long secondsUntilNextWindow(long windowSeconds) {
        long now = clock.instant().getEpochSecond();
        long elapsed = now % windowSeconds;
        return elapsed == 0 ? windowSeconds : windowSeconds - elapsed;
    }
}
