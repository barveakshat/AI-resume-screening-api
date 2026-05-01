package com.resumescreening.api.integration;

import com.resumescreening.api.security.ratelimit.RateLimitDecision;
import com.resumescreening.api.security.ratelimit.RateLimitRule;
import com.resumescreening.api.security.ratelimit.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RedisRateLimitIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void rateLimitUsesRedisCountersAndTtl() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        RateLimitRule rule = new RateLimitRule("integration-test", 2, Duration.ofMinutes(1));

        RateLimitDecision first = rateLimitService.checkLimit("user", "42", rule);
        RateLimitDecision second = rateLimitService.checkLimit("user", "42", rule);
        RateLimitDecision third = rateLimitService.checkLimit("user", "42", rule);

        assertThat(first.allowed()).isTrue();
        assertThat(second.allowed()).isTrue();
        assertThat(third.allowed()).isFalse();
        assertThat(third.remaining()).isZero();
    }
}
