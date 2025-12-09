# ADR-001: Use Redis for Caching Layer

**Status:** Accepted
**Date:** 2024-12
**Deciders:** Course Architecture Team
**Tags:** caching, redis, performance

## Context

The Product Catalog Service is experiencing performance issues with read-heavy workloads. Database queries are taking 50-100ms per request, and the database is becoming a bottleneck under load.

**Current metrics:**
- 90% of requests are reads
- Average database query time: 75ms
- Target response time: < 20ms
- Database CPU utilization: 85% during peak hours

**Requirements:**
- Reduce read latency to support 10x traffic increase
- Maintain data consistency (eventual consistency acceptable)
- Support multiple data structures (not just simple key-value)
- Enable advanced patterns (rate limiting, leaderboards, distributed locks)
- Keep infrastructure costs reasonable

## Decision

We will use **Redis** as an in-memory caching layer between the application and PostgreSQL database.

**Implementation approach:**
- Cache-Aside pattern for product catalog
- 5-minute TTL for product data
- Event-based invalidation on updates
- Spring Cache abstraction with Redis backend
- Redisson for advanced features (locks, rate limiting)

## Consequences

### Positive
- **10x faster reads**: 75ms → 5-10ms response time (cache hits)
- **Reduced database load**: 70-80% of reads served from cache
- **Horizontal scalability**: Multiple app instances share cache
- **Advanced features**: Rate limiting, leaderboards, distributed locks
- **Rich data structures**: Lists, Sets, Sorted Sets, Hashes
- **Persistence options**: RDB/AOF for recovery after restart
- **Active community**: Large ecosystem, well-documented

### Negative
- **Additional infrastructure**: Redis server adds operational complexity
- **Memory costs**: In-memory storage more expensive than disk
- **Eventual consistency**: Cached data may be stale until TTL expires
- **Cache invalidation complexity**: Need to carefully manage cache updates
- **New failure mode**: Cache unavailability impacts performance (though not correctness)

### Neutral
- **Learning curve**: Team needs to learn Redis operations and patterns
- **Monitoring requirements**: Need to track cache hit ratio, memory usage

## Alternatives Considered

### Alternative 1: Memcached
**Pros:**
- Simple and fast
- Lower memory usage than Redis
- Multi-threaded (better CPU utilization)

**Cons:**
- Only supports simple key-value storage
- No persistence (data lost on restart)
- No advanced data structures (can't do leaderboards, rate limiting)
- Limited eviction policies (LRU only)

**Rejected because:**
We need advanced features beyond simple caching (leaderboards with sorted sets, rate limiting with counters, distributed locks). Memcached is too limited for our use cases.

### Alternative 2: In-Memory Application Cache (e.g., Caffeine)
**Pros:**
- No external dependency
- Lowest latency (local memory)
- No network overhead

**Cons:**
- Not shared across instances (cache duplication)
- Invalidation complexity in distributed setup
- Doesn't help with distributed coordination
- Memory usage scales with instance count

**Rejected because:**
We run multiple application instances for high availability. Local caches would duplicate data and create invalidation challenges. We also need distributed features like locks and rate limiting.

### Alternative 3: PostgreSQL Query Results Caching
**Pros:**
- No additional infrastructure
- Automatic cache invalidation
- ACID guarantees

**Cons:**
- Still requires database connection for every request
- Limited to database queries only
- Doesn't help with rate limiting or distributed locks
- Adds complexity to database tuning

**Rejected because:**
Database-level caching doesn't reduce connection overhead or query parsing time. We need application-level caching for better performance and to support non-database use cases.

## Trade-Off Analysis

| Quality Attribute | Redis | Memcached | Local Cache | DB Caching |
|-------------------|-------|-----------|-------------|------------|
| Read Performance  | 5-10ms | 3-5ms | 1-2ms | 50-100ms |
| Scalability       | Excellent | Excellent | Poor | Limited |
| Complexity        | Medium | Low | Low | Low |
| Cost              | $50/mo | $30/mo | $0 | $0 |
| Feature Set       | Rich | Basic | Basic | Basic |
| Consistency       | Eventual | Eventual | Eventual | Strong |

## Risks and Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Redis unavailability causes service degradation | Medium | Medium | Fail open: serve from database if Redis down. Implement circuit breaker. |
| Memory exhaustion causing evictions | Medium | Medium | Set maxmemory with allkeys-lru policy. Monitor memory usage. Alert at 80% capacity. |
| Stale data in cache | High | Low | Use reasonable TTLs (5 min). Implement event-based invalidation for critical updates. |
| Cache stampede on popular items | Low | Medium | Implement locking on cache miss. Use probabilistic early expiration. |
| Increased infrastructure cost | Low | Low | Right-size instance based on metrics. Use compression for large values. |

## Implementation Notes

### Configuration
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
```

### Cache Configuration
```java
@Bean
public RedisCacheConfiguration cacheConfiguration() {
    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(5))
        .disableCachingNullValues()
        .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
}
```

### Service Layer
```java
@Cacheable(value = "products", key = "#id")
public Product getProduct(Long id) {
    return productRepository.findById(id)
        .orElseThrow(() -> new ProductNotFoundException(id));
}
```

### Monitoring
- Track cache hit ratio (target: > 70%)
- Monitor memory usage (alert at 80%)
- Measure cache operation latency
- Alert on cache connection failures

### Migration Path
1. Deploy Redis as read-through cache (cache-aside)
2. Monitor cache hit ratio for 1 week
3. Gradually increase TTL if consistency allows
4. Enable write-through for critical paths if needed

## References

- [Redis Official Documentation](https://redis.io/docs/)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Cache-Aside Pattern](https://docs.aws.amazon.com/whitepapers/latest/database-caching-strategies-using-redis/cache-aside.html)
- [Scaling Memcache at Facebook](https://research.facebook.com/publications/scaling-memcache-at-facebook/) - Similar challenges
- [CAP Theorem](https://en.wikipedia.org/wiki/CAP_theorem) - Consistency trade-offs
