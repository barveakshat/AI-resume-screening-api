package com.resumescreening.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTestService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void testRedisConnection() {
        try {
            // Test SET
            redisTemplate.opsForValue().set("test:connection", "Redis is working!", 60, TimeUnit.SECONDS);
            log.info("✅ Redis SET successful");

            // Test GET
            String value = (String) redisTemplate.opsForValue().get("test:connection");
            log.info("✅ Redis GET successful: {}", value);

            // Test DELETE
            redisTemplate.delete("test:connection");
            log.info("✅ Redis DELETE successful");

            log.info("🎉 Redis connection test PASSED!");
        } catch (Exception e) {
            log.error("❌ Redis connection test FAILED: {}", e.getMessage());
        }
    }
}