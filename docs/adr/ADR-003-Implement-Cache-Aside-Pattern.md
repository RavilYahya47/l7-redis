# ADR-003: Implement Cache-Aside Pattern for Product Catalog

**Status:** Accepted
**Date:** 2024-12
**Deciders:** Course Architecture Team
**Tags:** caching, pattern, consistency

## Context

We've decided to use Redis for caching (see ADR-001). Now we need to choose a caching pattern for the Product Catalog Service.

**Data characteristics:**
- **Read/Write ratio**: 90% reads, 10% writes
- **Update frequency**: Products updated a few times per day
- **Consistency requirement**: Eventual consistency acceptable (stale data for minutes is OK)
- **Data size**: 10-50KB per product
- **Access pattern**: Uniform distribution (no extreme hot keys)

**Caching patterns to consider:**
1. Cache-Aside (Lazy Loading)
2. Read-Through
3. Write-Through
4. Write-Behind (Write-Back)
5. Refresh-Ahead

## Decision

We will implement the **Cache-Aside pattern** with **event-based invalidation** for product data.

**Implementation:**
- Read: Check cache → miss: load from DB + populate cache → return
- Write: Update DB → invalidate cache → return
- TTL: 5 minutes as safety net
- Invalidation: On product update/delete

## Consequences

### Positive
- **Simplest to implement**: Spring `@Cacheable` and `@CacheEvict` annotations
- **Resilient**: Cache failure doesn't prevent database access
- **Efficient memory use**: Only caches requested items
- **Predictable behavior**: Clear data flow
- **Spring integration**: Built-in support with minimal code

### Negative
- **Cache miss penalty**: First request to each product slower (75ms)
- **Potential cache stampede**: Many requests for expired popular item
- **Stale data window**: Up to 5 minutes (or until invalidation)
- **Double write on invalidation**: Update DB + invalidate cache

### Neutral
- **Manual invalidation**: Need to remember to invalidate on writes
- **No automatic warming**: Cache starts empty

## Alternatives Considered

### Alternative 1: Read-Through Cache
**How it works:**
Application reads from cache. Cache automatically loads from DB on miss.

**Pros:**
- Simpler application code (cache handles misses)
- Consistent read path

**Cons:**
- Requires Redis Module or custom implementation
- Tighter coupling between cache and database
- Cache needs database credentials
- Harder to debug

**Rejected because:**
Adds complexity for minimal benefit. Spring's `@Cacheable` provides similar functionality with better visibility.

### Alternative 2: Write-Through Cache
**How it works:**
Application writes to cache. Cache synchronously writes to database.

**Pros:**
- Cache always consistent with database
- No cache misses after writes

**Cons:**
- Write latency doubled (cache + DB)
- Wasted cache entries for rarely-read data
- More complex error handling (what if cache write succeeds but DB fails?)

**Rejected because:**
Our workload is read-heavy. Doubling write latency for 10% of requests to optimize 90% doesn't make sense. Cache-Aside with invalidation is simpler and more appropriate.

### Alternative 3: Write-Behind (Write-Back)
**How it works:**
Application writes to cache. Cache asynchronously writes to database (batched).

**Pros:**
- Lowest write latency
- Can batch writes for efficiency
- Reduces database load

**Cons:**
- Data loss risk if cache crashes before DB write
- Complex implementation
- Eventual consistency (unbounded window)
- Not supported by Spring Cache natively

**Rejected because:**
Consistency and durability requirements don't justify the complexity. Losing writes is unacceptable for product catalog. The write performance gain (on 10% of requests) isn't worth the risk.

### Alternative 4: Refresh-Ahead
**How it works:**
Background job refreshes cache before expiration for popular items.

**Pros:**
- No cache miss penalty for popular items
- Predictable performance

**Cons:**
- Complex to implement (predict popularity)
- Wastes resources on unpopular items
- Doesn't eliminate invalidation need

**Rejected because:**
Access pattern doesn't show extreme hot keys. Complexity not justified. Could be added later if profiling shows hot keys.

## Trade-Off Analysis

| Quality Attribute | Cache-Aside | Read-Through | Write-Through | Write-Behind | Refresh-Ahead |
|-------------------|-------------|--------------|---------------|--------------|---------------|
| Implementation Complexity | Low | Medium | Medium | High | High |
| Write Performance | Good | Good | Poor | Excellent | Good |
| Read Performance | Good | Good | Excellent | Good | Excellent |
| Consistency | Eventual | Eventual | Strong | Eventual | Eventual |
| Failure Resilience | Excellent | Good | Poor | Poor | Good |
| Memory Efficiency | Excellent | Excellent | Poor | Poor | Poor |

## Risks and Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Cache stampede on popular items | Low | Medium | Implement lock-on-miss pattern if observed. Monitor cache miss rate. |
| Stale data served (> 5min) | Low | Low | Event-based invalidation on updates. Users accept eventual consistency. |
| Cache miss slowing down requests | Medium | Low | Acceptable trade-off. Monitor p99 latency. |
| Forgot to invalidate cache | Low | Medium | Code reviews. Integration tests. Cache expiration as safety net. |

## Implementation Notes

### Service Layer
```java
@Service
public class ProductService {

    // Cache-Aside Read
    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProduct(Long id) {
        // Cache miss: Load from database
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        return mapToResponse(product);
    }

    // Event-Based Invalidation on Write
    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        // Update database
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product = productRepository.save(product);

        // Cache automatically evicted by @CacheEvict
        return mapToResponse(product);
    }

    // Delete also evicts cache
    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
```

### Cache Configuration
```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(5))  // 5-minute TTL as safety net
        .disableCachingNullValues();      // Don't cache null results

    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
    cacheConfigurations.put("products", config);

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(config)
        .withInitialCacheConfigurations(cacheConfigurations)
        .build();
}
```

### Handling Cache Stampede (if needed)
```java
// Locking variant for hot keys
public ProductResponse getProduct(Long id) {
    ProductResponse cached = checkCache(id);
    if (cached != null) return cached;

    // Only one thread loads from database
    return lockService.executeWithLock("product:" + id, () -> {
        // Double-check cache after acquiring lock
        ProductResponse doubleCheck = checkCache(id);
        if (doubleCheck != null) return doubleCheck;

        // Load from database and cache
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        ProductResponse response = mapToResponse(product);
        putInCache(id, response);
        return response;
    });
}
```

### Monitoring
Track these metrics:
- **Cache hit ratio**: Target > 70%
- **Cache miss latency**: p50, p95, p99
- **Cache size**: Number of keys
- **Eviction rate**: Should be low

Prometheus queries:
```promql
# Hit ratio
cache_gets{name="products",result="hit"} / cache_gets{name="products"}

# Miss latency
histogram_quantile(0.95, cache_miss_latency{name="products"})
```

## References

- [Cache-Aside Pattern](https://docs.aws.amazon.com/whitepapers/latest/database-caching-strategies-using-redis/cache-aside.html)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Caching at Reddit](https://redditblog.com/2017/01/17/caching-at-reddit/)
- [Facebook Memcache Paper](https://research.facebook.com/publications/scaling-memcache-at-facebook/)
