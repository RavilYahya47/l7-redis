package com.course.lesson7.redis.controller;

import com.course.lesson7.redis.service.DistributedLockService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/locks")
@RequiredArgsConstructor
@Slf4j
public class DistributedLockController {

    private final DistributedLockService lockService;

    @PostMapping("/{lockKey}/execute")
    public ResponseEntity<LockResult> executeWithLock(
            @PathVariable String lockKey,
            @RequestParam(defaultValue = "5") long waitTime,
            @RequestParam(defaultValue = "10") long leaseTime,
            @RequestParam(defaultValue = "SECONDS") String timeUnit) {

        TimeUnit unit = TimeUnit.valueOf(timeUnit);

        String result = lockService.executeWithLock(
                lockKey,
                waitTime,
                leaseTime,
                unit,
                () -> {
                    log.info("Executing critical section with lock: {}", lockKey);
                    try {
                        Thread.sleep(2000); // Simulate work
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "Operation completed successfully";
                }
        );

        return ResponseEntity.ok(new LockResult(result != null, result));
    }

    @PostMapping("/{lockKey}/try")
    public ResponseEntity<LockResult> tryLock(
            @PathVariable String lockKey,
            @RequestParam(defaultValue = "10") long leaseTime,
            @RequestParam(defaultValue = "SECONDS") String timeUnit) {

        TimeUnit unit = TimeUnit.valueOf(timeUnit);
        boolean acquired = lockService.tryLock(lockKey, leaseTime, unit);

        return ResponseEntity.ok(new LockResult(
                acquired,
                acquired ? "Lock acquired" : "Lock already held"
        ));
    }

    @PostMapping("/{lockKey}/unlock")
    public ResponseEntity<Void> unlock(@PathVariable String lockKey) {
        lockService.unlock(lockKey);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{lockKey}/status")
    public ResponseEntity<Boolean> isLocked(@PathVariable String lockKey) {
        boolean locked = lockService.isLocked(lockKey);
        return ResponseEntity.ok(locked);
    }

    @Data
    @AllArgsConstructor
    static class LockResult {
        private boolean success;
        private String message;
    }
}
