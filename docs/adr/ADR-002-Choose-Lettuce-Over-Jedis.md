# ADR-002: Choose Lettuce Over Jedis as Redis Client

**Status:** Accepted
**Date:** 2024-12
**Deciders:** Course Architecture Team
**Tags:** redis, client, performance

## Context

We need to choose a Redis client library for Spring Boot application. The two most popular options for Java are:

1. **Jedis** - Synchronous, blocking client
2. **Lettuce** - Asynchronous, non-blocking client based on Netty

**Requirements:**
- Connection pooling for concurrent requests
- Thread-safe operations
- Good performance under load
- Integration with Spring Data Redis
- Support for Redis Cluster and Sentinel

## Decision

We will use **Lettuce** as the Redis client library.

Spring Boot 2.x+ uses Lettuce as the default Redis client, and we will continue with this default.

## Consequences

### Positive
- **Non-blocking I/O**: Better resource utilization under load
- **Thread-safe**: Single connection shared across threads
- **Reactive support**: Enables reactive programming with Spring WebFlux (future expansion)
- **Lower resource usage**: Fewer connections needed than Jedis
- **Default in Spring Boot**: No extra configuration needed
- **Active development**: Regular updates and bug fixes
- **Pipelining support**: Better batch operation performance

### Negative
- **Steeper learning curve**: Asynchronous programming more complex
- **Debugging complexity**: Stack traces can be harder to follow
- **Slightly higher latency for single operations**: Async overhead (< 1ms difference)

### Neutral
- **Mature ecosystem**: Both Jedis and Lettuce are production-ready
- **Similar API**: Migration between clients relatively straightforward

## Alternatives Considered

### Alternative 1: Jedis
**Pros:**
- Simpler programming model (synchronous)
- Lower latency for single operations
- Easier to debug
- Been around longer (more battle-tested)

**Cons:**
- Not thread-safe (need connection pool)
- Higher resource usage (more connections)
- Blocking I/O (thread per connection)
- No reactive support
- Less active development recently

**Rejected because:**
Lettuce's non-blocking architecture is better suited for high-concurrency scenarios. The slight complexity increase is worth the scalability and resource efficiency gains.

### Alternative 2: Redisson
**Pros:**
- Highest-level abstraction
- Built-in distributed objects (locks, queues, etc.)
- Automatic retry and failover
- Rich feature set

**Cons:**
- Heavier dependency
- More opinionated
- Overkill for simple caching
- Steeper learning curve

**Decision:**
We use **both Lettuce and Redisson**:
- **Lettuce** (via Spring Data Redis) for caching
- **Redisson** for distributed locks and advanced features

This gives us Spring integration for caching and Redisson's powerful distributed primitives for coordination.

## Trade-Off Analysis

| Feature | Lettuce | Jedis | Redisson |
|---------|---------|-------|----------|
| Async/Non-blocking | Yes | No | Yes |
| Thread-safe | Yes | No (need pool) | Yes |
| Connection efficiency | Excellent | Good | Excellent |
| Spring Boot default | Yes | No | No |
| Reactive support | Yes | No | Yes |
| Learning curve | Medium | Low | High |
| Distributed objects | No | No | Yes |

## Risks and Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Team unfamiliar with async programming | Medium | Low | Provide training. Use synchronous APIs where possible. |
| Debugging async operations more difficult | Medium | Low | Enable detailed logging. Use monitoring tools. |
| Connection pool misconfiguration | Low | Medium | Use Spring Boot defaults. Monitor connection metrics. |

## Implementation Notes

### Spring Boot Configuration (application.yml)
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      lettuce:
        pool:
          max-active: 8    # Max connections
          max-idle: 8      # Max idle connections
          min-idle: 2      # Min idle connections
          max-wait: -1ms   # Infinite wait
```

### Connection Pool Sizing
Based on concurrency requirements:
- **Low concurrency** (< 10 concurrent requests): 2-4 connections
- **Medium concurrency** (10-50 concurrent requests): 8-16 connections
- **High concurrency** (50+ concurrent requests): 16-32 connections

Formula: `max-active = max expected concurrent requests / 2`

### RedisTemplate Bean
```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    // Lettuce is used by default via connectionFactory
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    return template;
}
```

### Monitoring Metrics
Track via Spring Boot Actuator:
- `lettuce.connections.active` - Active connections
- `lettuce.connections.idle` - Idle connections
- `lettuce.command.latency` - Command latency

### When to Use Sync vs Async
**Use synchronous (default):**
- Simple CRUD operations
- Business logic requiring immediate results
- Most caching scenarios

**Use asynchronous:**
- High-throughput scenarios
- Non-critical operations
- Batch processing
- Reactive applications

## References

- [Lettuce Documentation](https://lettuce.io/)
- [Jedis GitHub](https://github.com/redis/jedis)
- [Spring Data Redis Lettuce](https://docs.spring.io/spring-data/redis/docs/current/reference/html/#redis:connectors:lettuce)
- [Lettuce vs Jedis Comparison](https://stackoverflow.com/questions/45867460/jedis-vs-lettuce)
- [Spring Boot Redis Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/data.html#data.nosql.redis)
