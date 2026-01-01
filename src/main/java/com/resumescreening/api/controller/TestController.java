package com.resumescreening.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/redis")
    public Map<String, String> testRedis() {
        Map<String, String> response = new HashMap<>();

        try {
            // Test write
            redisTemplate.opsForValue().set("test-key", "Redis is working!");

            // Test read
            String value = (String) redisTemplate.opsForValue().get("test-key");

            response.put("status", "success");
            response.put("message", "Redis connection successful");
            response.put("value", value);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Application is running");
        return response;
    }
}
