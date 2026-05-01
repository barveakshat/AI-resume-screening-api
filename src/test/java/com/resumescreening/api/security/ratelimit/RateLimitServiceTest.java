package com.resumescreening.api.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitServiceTest {

    @Test
    void firstRequestCreatesKeyWithTtlAndRemainingCount() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(any(String.class))).thenReturn(1L);

        RateLimitService service = new RateLimitService(redisTemplate);
        RateLimitDecision decision = service.checkLimit("user", "1", new RateLimitRule("test", 2, Duration.ofMinutes(1)));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isEqualTo(1L);
        verify(redisTemplate).expire(any(String.class), eq(Duration.ofMinutes(1)));
    }

    @Test
    void requestOverLimitIsBlocked() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(any(String.class))).thenReturn(3L);

        RateLimitService service = new RateLimitService(redisTemplate);
        RateLimitDecision decision = service.checkLimit("ip", "127.0.0.1", new RateLimitRule("test", 2, Duration.ofMinutes(1)));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.remaining()).isZero();
    }
}
