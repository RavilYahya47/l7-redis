package com.course.lesson7.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String TOKEN_BUCKET_PREFIX = "token_bucket:";

    public boolean isAllowedFixedWindow(String key, int maxRequests, long windowSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + "fixed:" + key;

        try {
            Long count = redisTemplate.opsForValue().increment(redisKey);

            if (count == null) {
                return false;
            }

            if (count == 1) {
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
            }

            boolean allowed = count <= maxRequests;

            if (!allowed) {
                log.warn("Rate limit exceeded for key: {} (count: {}/{})", key, count, maxRequests);
            }

            return allowed;

        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
            return true;
        }
    }

    public boolean isAllowedSlidingWindow(String key, int maxRequests, long windowSeconds) {
        String redisKey = RATE_LIMIT_PREFIX + "sliding:" + key;
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000);

        try {
            redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);

            Long count = redisTemplate.opsForZSet().zCard(redisKey);

            if (count != null && count >= maxRequests) {
                log.warn("Sliding window rate limit exceeded for key: {} (count: {}/{})",
                        key, count, maxRequests);
                return false;
            }

            redisTemplate.opsForZSet().add(redisKey, String.valueOf(now), now);

            redisTemplate.expire(redisKey, windowSeconds * 2, TimeUnit.SECONDS);

            return true;

        } catch (Exception e) {
            log.error("Error in sliding window rate limit for key: {}", key, e);
            return true;
        }
    }

    public boolean isAllowedTokenBucket(String key, int capacity, double refillRate, int tokensRequired) {
        String tokensKey = TOKEN_BUCKET_PREFIX + key + ":tokens";
        String timestampKey = TOKEN_BUCKET_PREFIX + key + ":timestamp";

        try {
            Object tokensObj = redisTemplate.opsForValue().get(tokensKey);
            Object timestampObj = redisTemplate.opsForValue().get(timestampKey);

            long now = System.currentTimeMillis();

            double tokens = capacity;
            if (tokensObj instanceof Number) {
                tokens = ((Number) tokensObj).doubleValue();
            } else if (tokensObj instanceof String) {
                tokens = Double.parseDouble((String) tokensObj);
            }

            long lastRefill = now;
            if (timestampObj instanceof Number) {
                lastRefill = ((Number) timestampObj).longValue();
            } else if (timestampObj instanceof String) {
                lastRefill = Long.parseLong((String) timestampObj);
            }

            double elapsedSeconds = (now - lastRefill) / 1000.0;
            tokens = Math.min(capacity, tokens + (elapsedSeconds * refillRate));

            boolean allowed = tokens >= tokensRequired;

            if (allowed) {
                tokens -= tokensRequired;
                redisTemplate.opsForValue().set(tokensKey, String.valueOf(tokens), Duration.ofMinutes(10));
                redisTemplate.opsForValue().set(timestampKey, String.valueOf(now), Duration.ofMinutes(10));
            } else {
                log.warn("Token bucket rate limit exceeded for key: {} (tokens: {}, required: {})",
                        key, tokens, tokensRequired);
            }

            return allowed;

        } catch (Exception e) {
            log.error("Error in token bucket rate limit for key: {}", key, e);
            return true;
        }
    }

    public long getRemainingRequests(String key, int maxRequests) {
        String redisKey = RATE_LIMIT_PREFIX + "fixed:" + key;
        Object countObj = redisTemplate.opsForValue().get(redisKey);
        long count = 0;

        if (countObj instanceof Number) {
            count = ((Number) countObj).longValue();
        } else if (countObj instanceof String) {
            count = Long.parseLong((String) countObj);
        }

        return Math.max(0, maxRequests - count);
    }

    public void reset(String key) {
        redisTemplate.delete(RATE_LIMIT_PREFIX + "fixed:" + key);
        redisTemplate.delete(RATE_LIMIT_PREFIX + "sliding:" + key);
        redisTemplate.delete(TOKEN_BUCKET_PREFIX + key + ":tokens");
        redisTemplate.delete(TOKEN_BUCKET_PREFIX + key + ":timestamp");
        log.info("Rate limit reset for key: {}", key);
    }
}
