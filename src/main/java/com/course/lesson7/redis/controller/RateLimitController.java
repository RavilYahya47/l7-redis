package com.course.lesson7.redis.controller;

import com.course.lesson7.redis.service.RateLimitService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rate-limit")
@RequiredArgsConstructor
public class RateLimitController {

    private final RateLimitService rateLimitService;

    @GetMapping("/fixed-window/{userId}")
    public ResponseEntity<RateLimitResponse> testFixedWindow(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int maxRequests,
            @RequestParam(defaultValue = "60") long windowSeconds) {

        boolean allowed = rateLimitService.isAllowedFixedWindow(userId, maxRequests, windowSeconds);
        long remaining = rateLimitService.getRemainingRequests(userId, maxRequests);

        return ResponseEntity
                .status(allowed ? HttpStatus.OK : HttpStatus.TOO_MANY_REQUESTS)
                .body(new RateLimitResponse(allowed, remaining, "Fixed Window"));
    }

    @GetMapping("/sliding-window/{userId}")
    public ResponseEntity<RateLimitResponse> testSlidingWindow(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int maxRequests,
            @RequestParam(defaultValue = "60") long windowSeconds) {

        boolean allowed = rateLimitService.isAllowedSlidingWindow(userId, maxRequests, windowSeconds);

        return ResponseEntity
                .status(allowed ? HttpStatus.OK : HttpStatus.TOO_MANY_REQUESTS)
                .body(new RateLimitResponse(allowed, null, "Sliding Window"));
    }

    @GetMapping("/token-bucket/{userId}")
    public ResponseEntity<RateLimitResponse> testTokenBucket(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int capacity,
            @RequestParam(defaultValue = "1.0") double refillRate,
            @RequestParam(defaultValue = "1") int tokensRequired) {

        boolean allowed = rateLimitService.isAllowedTokenBucket(
                userId, capacity, refillRate, tokensRequired);

        return ResponseEntity
                .status(allowed ? HttpStatus.OK : HttpStatus.TOO_MANY_REQUESTS)
                .body(new RateLimitResponse(allowed, null, "Token Bucket"));
    }

    @PostMapping("/reset/{userId}")
    public ResponseEntity<Void> resetRateLimit(@PathVariable String userId) {
        rateLimitService.reset(userId);
        return ResponseEntity.ok().build();
    }

    @Data
    @AllArgsConstructor
    static class RateLimitResponse {
        private boolean allowed;
        private Long remaining;
        private String algorithm;
    }
}
