package com.example.authentication_module.Service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Fixed-window request counter backed by Redis. Each call to {@link #hit}
 * increments the counter for {@code key} and reports whether the caller is
 * still within the allowed limit for the current window.
 */
@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String key, int maxAttempts, Duration window) {
        return increment(key, window) <= maxAttempts;
    }

    public long increment(String key, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }

        return count == null ? 0L : count;
    }

    public long getCount(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }

    public void reset(String key) {
        redisTemplate.delete(key);
    }
}
