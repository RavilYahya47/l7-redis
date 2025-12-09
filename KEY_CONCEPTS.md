# Key Concepts: Redis Caching Patterns

## Table of Contents

1. [Redis Fundamentals](#redis-fundamentals)
2. [Caching Patterns](#caching-patterns)
3. [Cache Invalidation Strategies](#cache-invalidation-strategies)
4. [Redis Data Structures](#redis-data-structures)
5. [Rate Limiting Algorithms](#rate-limiting-algorithms)
6. [Distributed Locks](#distributed-locks)
7. [Session Management](#session-management)
8. [Performance Optimization](#performance-optimization)
9. [Common Pitfalls](#common-pitfalls)

---

## Redis Fundamentals

### What is Redis?

**Redis** (Remote Dictionary Server) is an in-memory data structure store used as:
- **Cache** - Fast temporary data storage
- **Database** - Persistent data storage with snapshotting
- **Message Broker** - Pub/Sub messaging
- **Session Store** - Distributed session management

### Why Redis?

**Performance:**
- In-memory storage: microsecond latency
- Single-threaded: no lock contention
- Efficient data structures: O(1) for most operations

**Versatility:**
- Multiple data structures (Strings, Lists, Sets, Sorted Sets, Hashes)
- Atomic operations
- Built-in TTL (Time-To-Live)
- Lua scripting for complex operations

**Scalability:**
- Replication for read scaling
- Clustering for horizontal scaling
- Sentinel for high availability

### Redis vs Memcached

| Feature | Redis | Memcached |
|---------|-------|-----------|
| Data Structures | Multiple (String, List, Set, ZSet, Hash) | Only String |
| Persistence | Yes (RDB, AOF) | No |
| Replication | Yes | No |
| Transactions | Yes | No |
| Lua Scripting | Yes | No |
| Memory Eviction | 6+ policies | LRU only |
| Use Case | Cache + data structure server | Simple cache |

**When to use Redis over Memcached:**
- Need complex data structures (leaderboards, queues)
- Need persistence (session recovery after restart)
- Need atomic operations on complex data
- Need pub/sub messaging
- Need distributed locks

---

## Caching Patterns

### 1. Cache-Aside (Lazy Loading)

**How it works:**
1. Application checks cache first
2. On cache miss, load from database
3. Populate cache with loaded data
4. Return data to caller

```java
@Cacheable(value = "products", key = "#id")
public Product getProduct(Long id) {
    // Cache miss triggers database query
    return productRepository.findById(id)
        .orElseThrow(() -> new ProductNotFoundException(id));
}
```

**Advantages:**
- Only caches requested data (no wasted memory)
- Cache failure doesn't bring down system
- Simple to implement with Spring Cache

**Disadvantages:**
- Cache miss penalty (first request slower)
- Possible cache stampede on popular items
- Stale data until cache expires

**Best for:**
- Read-heavy workloads
- Data that's not frequently updated
- Tolerance for eventual consistency

### 2. Write-Through

**How it works:**
1. Application writes to cache
2. Cache synchronously writes to database
3. Return success to caller

```java
@CachePut(value = "products", key = "#result.id")
public Product updateProduct(Product product) {
    // Updates both cache and database
    return productRepository.save(product);
}
```

**Advantages:**
- Cache always consistent with database
- No cache miss penalty on reads
- Data always fresh

**Disadvantages:**
- Write latency (2x write operations)
- Wasted writes if data never read
- More complex error handling

**Best for:**
- Write-heavy workloads
- Data that's frequently read after write
- Strong consistency requirements

### 3. Write-Behind (Write-Back)

**How it works:**
1. Application writes to cache
2. Cache immediately returns success
3. Cache asynchronously writes to database (batched)

**Advantages:**
- Lowest write latency
- Can batch database writes
- Reduces database load

**Disadvantages:**
- Risk of data loss if cache fails
- Complex implementation
- Eventual consistency

**Best for:**
- High write throughput requirements
- Can tolerate eventual consistency
- Logging, analytics, metrics

### 4. Refresh-Ahead

**How it works:**
1. Background job refreshes cache before expiration
2. Cache never actually expires for popular items
3. Users always get fast responses

**Advantages:**
- No cache miss penalty
- Always fresh data
- Predictable performance

**Disadvantages:**
- Complex to implement
- Wastes resources on unpopular items
- Needs predictive algorithm

**Best for:**
- Predictable access patterns
- Low tolerance for latency spikes
- Mission-critical data

---

## Cache Invalidation Strategies

> "There are only two hard things in Computer Science: cache invalidation and naming things." - Phil Karlton

### 1. Time-To-Live (TTL)

**Strategy:** Auto-expire cached data after fixed duration.

```java
@Cacheable(value = "products", key = "#id")
// TTL configured in RedisConfig: 5 minutes
public Product getProduct(Long id) { ... }
```

**Pros:**
- Simple to implement
- Guaranteed freshness bounds
- Automatic cleanup

**Cons:**
- Stale data until expiration
- Cache miss on expiration
- Fixed TTL may not suit all data

**Choose TTL based on:**
- **Seconds** - Real-time data (stock prices, live scores)
- **Minutes** - Frequently changing (inventory, trending)
- **Hours** - Relatively stable (product details, categories)
- **Days** - Rarely changing (static content, configuration)

### 2. Event-Based Invalidation

**Strategy:** Invalidate cache when underlying data changes.

```java
@CacheEvict(value = "products", key = "#id")
public Product updateProduct(Long id, UpdateRequest request) {
    // Update triggers cache invalidation
    return productRepository.save(product);
}
```

**Pros:**
- Cache always consistent
- No stale data
- Efficient (only invalidate what changed)

**Cons:**
- Tight coupling between operations
- Complex in distributed systems
- Requires careful orchestration

### 3. Tag-Based Invalidation

**Strategy:** Group related cache entries and invalidate by tag.

```java
// Invalidate all product caches when category changes
@CacheEvict(value = "products", allEntries = true)
public void updateCategory(Long categoryId) { ... }
```

**Pros:**
- Bulk invalidation
- Logical grouping
- Flexible control

**Cons:**
- May invalidate too much
- Needs tag management
- Overhead of tracking tags

### 4. Version-Based Invalidation

**Strategy:** Include version in cache key, change version on update.

```java
// Cache key: "product:123:v5"
String cacheKey = "product:" + id + ":v" + version;
```

**Pros:**
- No explicit invalidation needed
- Old versions naturally expire
- Supports gradual rollout

**Cons:**
- Multiple versions in cache
- Memory overhead
- Cleanup complexity

---

## Redis Data Structures

### 1. Strings

**Use cases:** Simple key-value storage, counters, flags

```java
// Set string value
redisTemplate.opsForValue().set("user:1000:name", "John Doe");

// Get string value
String name = redisTemplate.opsForValue().get("user:1000:name");

// Increment counter
Long views = redisTemplate.opsForValue().increment("product:1:views");
```

**Operations:** O(1) for GET, SET, INCR, DECR

### 2. Lists

**Use cases:** Queues, recent items, activity feeds

```java
// Push to list
redisTemplate.opsForList().leftPush("queue:orders", order);

// Pop from list
Order order = redisTemplate.opsForList().rightPop("queue:orders");

// Get range
List<String> recent = redisTemplate.opsForList().range("recent:items", 0, 9);
```

**Operations:** O(1) for push/pop at ends, O(N) for middle operations

### 3. Sets

**Use cases:** Unique items, tags, online users

```java
// Add to set
redisTemplate.opsForSet().add("tags:product:1", "electronics", "wireless");

// Check membership
Boolean isMember = redisTemplate.opsForSet().isMember("online:users", "user:1000");

// Set operations
Set<String> common = redisTemplate.opsForSet().intersect("likes:user1", "likes:user2");
```

**Operations:** O(1) for add, remove, membership check

### 4. Sorted Sets (ZSet)

**Use cases:** Leaderboards, rankings, time-series

```java
// Add with score
redisTemplate.opsForZSet().add("leaderboard:global", "player1", 1500.0);

// Get rank
Long rank = redisTemplate.opsForZSet().reverseRank("leaderboard:global", "player1");

// Get top N
Set<TypedTuple<Object>> top10 = redisTemplate.opsForZSet()
    .reverseRangeWithScores("leaderboard:global", 0, 9);
```

**Operations:** O(log N) for add, remove, rank queries

### 5. Hashes

**Use cases:** Objects, user profiles, settings

```java
// Set hash field
redisTemplate.opsForHash().put("user:1000", "email", "john@example.com");

// Get all fields
Map<Object, Object> user = redisTemplate.opsForHash().entries("user:1000");

// Increment field
Long cartTotal = redisTemplate.opsForHash().increment("cart:1000", "total", 1);
```

**Operations:** O(1) for field get/set, O(N) for get all fields

---

## Rate Limiting Algorithms

### 1. Fixed Window

**Algorithm:**
- Count requests in fixed time windows
- Reset counter at window boundary

**Implementation:**
```java
public boolean isAllowed(String key, int maxRequests, long windowSeconds) {
    Long count = redisTemplate.opsForValue().increment(key);
    if (count == 1) {
        redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
    }
    return count <= maxRequests;
}
```

**Pros:**
- Simple to implement
- Low memory usage
- Predictable resource usage

**Cons:**
- Burst at window boundaries (2x limit possible)
- Unfair to requests at boundary

**Example:**
```
Window 1: [10:00:00 - 10:00:59]
Window 2: [10:01:00 - 10:01:59]

Limit: 100 requests/minute
Possible: 100 at 10:00:59 + 100 at 10:01:00 = 200 in 2 seconds!
```

### 2. Sliding Window

**Algorithm:**
- Track individual request timestamps
- Count requests in last N seconds from now

**Implementation:**
```java
public boolean isAllowed(String key, int maxRequests, long windowSeconds) {
    long now = System.currentTimeMillis();
    long windowStart = now - (windowSeconds * 1000);

    // Remove old entries
    redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

    // Count current window
    Long count = redisTemplate.opsForZSet().zCard(key);
    if (count >= maxRequests) return false;

    // Add current request
    redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
    return true;
}
```

**Pros:**
- No boundary burst
- Fair distribution
- Accurate limiting

**Cons:**
- Higher memory usage (stores all timestamps)
- More expensive (O(log N) operations)

### 3. Token Bucket

**Algorithm:**
- Bucket holds tokens (capacity)
- Tokens added at fixed rate
- Each request consumes tokens

**Implementation:**
```java
public boolean isAllowed(String key, int capacity, double refillRate, int tokensRequired) {
    long now = System.currentTimeMillis();
    double tokens = getTokens(key);
    long lastRefill = getLastRefill(key);

    // Refill tokens based on elapsed time
    double elapsed = (now - lastRefill) / 1000.0;
    tokens = Math.min(capacity, tokens + (elapsed * refillRate));

    if (tokens >= tokensRequired) {
        tokens -= tokensRequired;
        saveTokens(key, tokens, now);
        return true;
    }
    return false;
}
```

**Pros:**
- Allows controlled bursts
- Smooth rate limiting
- Flexible (different token costs)

**Cons:**
- More complex
- Needs precise timing
- Possible clock skew issues

**Best for:** APIs with bursty traffic but sustained rate limits

### Algorithm Comparison

| Scenario | Best Algorithm | Reason |
|----------|---------------|--------|
| API rate limiting | Token Bucket | Allows bursts, smooth limiting |
| Login attempts | Fixed Window | Simple, strict enforcement |
| Real-time chat | Sliding Window | Fair, no boundary issues |
| Payment processing | Token Bucket | Controlled bursts, strict limits |

---

## Distributed Locks

### Why Distributed Locks?

In distributed systems with multiple application instances:
- Prevent duplicate processing
- Ensure only one instance modifies shared resource
- Coordinate complex workflows

### Redlock Algorithm

**Requirements for correct lock:**
1. **Safety** - Mutual exclusion (only one lock holder)
2. **Liveness A** - No deadlocks (lock eventually released)
3. **Liveness B** - Fault tolerance (works if majority nodes up)

### Implementation with Redisson

```java
public <T> T executeWithLock(String lockKey, Supplier<T> operation) {
    RLock lock = redissonClient.getLock(lockKey);

    try {
        // Try to acquire lock (wait 5s, auto-release 10s)
        boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);

        if (acquired) {
            return operation.get();
        } else {
            throw new LockAcquisitionException("Failed to acquire lock");
        }
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

### Lock Patterns

**1. Mutex Lock (Exclusive)**
- Only one holder at a time
- Use for: Inventory updates, order processing

**2. Fair Lock**
- FIFO order of acquisition
- Use for: Ticket booking, resource allocation

**3. Read/Write Lock**
- Multiple readers OR one writer
- Use for: Config updates, cache updates

**4. Semaphore**
- N holders at a time
- Use for: Rate limiting, connection pools

### Common Pitfalls

**Deadlocks:**
```java
// BAD: Can deadlock if two processes acquire in different order
lock1.acquire();
lock2.acquire();

// GOOD: Always acquire in same order
List<Lock> locks = Arrays.asList(lock1, lock2);
locks.sort(Comparator.comparing(Lock::getName));
locks.forEach(Lock::acquire);
```

**Lock Expiration:**
```java
// BAD: Lock might expire before operation completes
lock.tryLock(0, 5, SECONDS); // 5s auto-release
expensiveOperation(); // Takes 10s!

// GOOD: Set lease time longer than max operation time
lock.tryLock(0, 30, SECONDS); // 30s auto-release
expensiveOperation(); // Takes 10s max
```

---

## Session Management

### Why Redis for Sessions?

**Traditional (Sticky Sessions):**
- Sessions stored in application memory
- Load balancer routes user to same server
- Server failure loses sessions
- Can't scale horizontally

**Redis-Based Sessions:**
- Sessions stored in centralized Redis
- Any server can handle any request
- Server failure doesn't lose sessions
- Easy horizontal scaling

### Session Data Model

```java
public class SessionData {
    private String sessionId;
    private Long userId;
    private Map<String, Object> attributes;  // Shopping cart, preferences, etc.
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
}
```

### Session Expiration Strategies

**1. Fixed TTL**
```java
// Session expires 30 minutes after creation
redisTemplate.opsForValue().set(sessionKey, sessionData, 30, TimeUnit.MINUTES);
```

**2. Sliding TTL**
```java
// Reset TTL on each access
public SessionData getSession(String sessionId) {
    SessionData session = redisTemplate.opsForValue().get(sessionId);
    if (session != null) {
        // Extend session on each access
        redisTemplate.expire(sessionId, 30, TimeUnit.MINUTES);
    }
    return session;
}
```

**3. Hybrid (Absolute + Sliding)**
```java
// Max 8 hours absolute, but extend if active (up to limit)
public class SessionData {
    private LocalDateTime absoluteExpiry;  // Max 8 hours from creation
    private LocalDateTime slidingExpiry;    // 30 min from last access
}
```

### Session Storage Patterns

**Pattern 1: Single Key**
```redis
SET session:abc123 "{userId:1,cart:[...],prefs:{...}}"
```
- Simple
- Atomic updates
- Full session on every read

**Pattern 2: Hash**
```redis
HSET session:abc123 userId 1
HSET session:abc123 cartSize 3
HSET session:abc123 lastPage "/products"
```
- Granular updates
- Can read individual fields
- More operations for full session

---

## Performance Optimization

### 1. Connection Pooling

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 8    # Max connections
          max-idle: 8      # Max idle connections
          min-idle: 2      # Min idle connections
```

**Best practices:**
- Set `max-active` based on expected concurrency
- Keep `max-idle` high to avoid connection churn
- Monitor pool metrics for tuning

### 2. Pipelining

**Bad:** Multiple round trips
```java
for (String key : keys) {
    redisTemplate.opsForValue().get(key);  // N round trips
}
```

**Good:** Single round trip
```java
redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
    for (String key : keys) {
        connection.get(key.getBytes());  // 1 round trip
    }
    return null;
});
```

### 3. Compression

For large values, compress before storing:

```java
public void setCached(String key, Object value) {
    byte[] serialized = serialize(value);
    byte[] compressed = compress(serialized);
    redisTemplate.opsForValue().set(key, compressed);
}
```

**When to compress:**
- Values > 1KB
- Text-heavy data (JSON, XML)
- Network bandwidth limited

**When NOT to compress:**
- Small values (< 1KB)
- Already compressed (images, videos)
- CPU-constrained

---

## Common Pitfalls

### 1. Cache Stampede

**Problem:** Popular item expires, many requests hit database simultaneously.

**Solution: Lock on cache miss**
```java
public Product getProduct(Long id) {
    Product cached = cache.get(id);
    if (cached != null) return cached;

    // Only one thread loads from DB
    return lockService.executeWithLock("product:" + id, () -> {
        Product fromDb = repository.findById(id);
        cache.put(id, fromDb);
        return fromDb;
    });
}
```

### 2. Large Keys

**Problem:** Storing multi-megabyte objects blocks Redis (single-threaded).

**Solution: Break into smaller chunks or use compression**

### 3. Unbounded Collections

**Problem:** List/Set grows without limit, consumes all memory.

**Solution: Use TTL or periodic cleanup**
```java
// Keep only recent 1000 items
Long size = redisTemplate.opsForList().size(key);
if (size > 1000) {
    redisTemplate.opsForList().trim(key, 0, 999);
}
```

### 4. Hot Keys

**Problem:** One key gets all traffic, becomes bottleneck.

**Solution: Replicate hot data**
```java
// Shard hot key across multiple keys
String shardKey = "popular_product:" + (userId % 10);
```

### 5. Ignoring Eviction Policies

**Redis eviction policies:**
- `noeviction` - Return errors when memory full
- `allkeys-lru` - Evict least recently used (any key)
- `volatile-lru` - Evict least recently used (keys with TTL)
- `allkeys-random` - Evict random key
- `volatile-ttl` - Evict soonest to expire

**Configure based on use case:**
```bash
# In docker-compose.yml
command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
```

---

**Next:** Apply these concepts in [EXERCISES.md](EXERCISES.md) for hands-on practice!
