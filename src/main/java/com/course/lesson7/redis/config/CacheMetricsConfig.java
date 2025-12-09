package com.course.lesson7.redis.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheDecorator;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCache;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class CacheMetricsConfig {

    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void bindCacheMetrics() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {

                if (cache instanceof TransactionAwareCacheDecorator) {
                    cache = ((TransactionAwareCacheDecorator) cache).getTargetCache();
                }

                if (cache instanceof RedisCache redisCache) {
                    meterRegistry.gauge("cache.size." + cacheName, redisCache,
                        c -> (double) c.getNativeCache().toString().length());
                }
            }
        });
    }
}
