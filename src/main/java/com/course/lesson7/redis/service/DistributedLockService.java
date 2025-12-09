package com.course.lesson7.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "lock:";

    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime,
                                  TimeUnit timeUnit, Supplier<T> operation) {
        String redisKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(redisKey);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);

            if (acquired) {
                log.info("Lock acquired: {}", lockKey);
                try {
                    return operation.get();
                } finally {
                    lock.unlock();
                    log.info("Lock released: {}", lockKey);
                }
            } else {
                log.warn("Failed to acquire lock: {}", lockKey);
                return null;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for lock: {}", lockKey, e);
            return null;
        } catch (Exception e) {
            log.error("Error executing with lock: {}", lockKey, e);
            throw new RuntimeException("Lock operation failed", e);
        }
    }

    public boolean tryLock(String lockKey, long leaseTime, TimeUnit timeUnit) {
        String redisKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(redisKey);

        try {
            boolean acquired = lock.tryLock(0, leaseTime, timeUnit);
            if (acquired) {
                log.info("Lock acquired immediately: {}", lockKey);
            } else {
                log.debug("Lock already held: {}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void unlock(String lockKey) {
        String redisKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(redisKey);

        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("Lock released: {}", lockKey);
        } else {
            log.warn("Attempted to unlock a lock not held by current thread: {}", lockKey);
        }
    }

    public boolean isLocked(String lockKey) {
        String redisKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(redisKey);
        return lock.isLocked();
    }

    public <T> T executeWithFairLock(String lockKey, long waitTime, long leaseTime,
                                      TimeUnit timeUnit, Supplier<T> operation) {
        String redisKey = LOCK_PREFIX + "fair:" + lockKey;
        RLock lock = redissonClient.getFairLock(redisKey);

        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);

            if (acquired) {
                log.info("Fair lock acquired: {}", lockKey);
                try {
                    return operation.get();
                } finally {
                    lock.unlock();
                    log.info("Fair lock released: {}", lockKey);
                }
            } else {
                log.warn("Failed to acquire fair lock: {}", lockKey);
                return null;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for fair lock: {}", lockKey, e);
            return null;
        }
    }
}
