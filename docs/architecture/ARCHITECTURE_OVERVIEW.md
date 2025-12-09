# Architecture Overview: Redis Caching Patterns

## System Context

The Redis Caching Patterns application is an educational microservice demonstrating production-grade caching patterns and Redis usage. It implements a Product Catalog Service with multiple Redis-powered features.

## Architectural Goals

### Functional Requirements
- Browse and search product catalog
- Manage product information (CRUD)
- Track product view counts
- Real-time leaderboards
- User session management
- API rate limiting
- Distributed operation coordination

### Non-Functional Requirements

| Quality Attribute | Target | Current |
|-------------------|--------|---------|
| Response Time (cached) | < 20ms | 5-15ms |
| Response Time (uncached) | < 100ms | 50-100ms |
| Cache Hit Ratio | > 70% | ~80% |
| Availability | 99.9% | 99.5% |
| Throughput | 1000 req/s | 500 req/s |
| Data Freshness | < 5 min | 5 min |

## Architectural Style: Layered Architecture with Caching

```
┌─────────────────────────────────────────────────┐
│            REST API Layer                        │
│  (ProductController, RateLimitController, etc.) │
└─────────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────┐
│          Service Layer                           │
│  (ProductService, RateLimitService, etc.)       │
│  ├─ Business Logic                              │
│  └─ Caching Logic (@Cacheable, @CacheEvict)    │
└─────────────────────────────────────────────────┘
         │                      │
         │                      │
         ▼                      ▼
┌──────────────────┐  ┌──────────────────┐
│  Cache Layer     │  │  Data Layer      │
│  (Redis)         │  │  (PostgreSQL)    │
│  ├─ Product Cache│  │  ├─ Products     │
│  ├─ Sessions     │  │  ├─ Categories   │
│  ├─ Leaderboards │  │  └─ Users        │
│  ├─ Rate Limits  │  │                  │
│  └─ Locks        │  │                  │
└──────────────────┘  └──────────────────┘
```

## Key Architectural Decisions

See [Architecture Decision Records](../adr/) for detailed rationale.

| Decision | Rationale | Trade-off |
|----------|-----------|-----------|
| Use Redis for caching | 10x performance gain, rich features | Added complexity, eventual consistency |
| Choose Lettuce client | Non-blocking I/O, Spring Boot default | Slightly higher latency per op |
| Cache-Aside pattern | Simple, resilient, Spring integration | Cache miss penalty |
| 5-minute TTL | Balance freshness vs performance | Stale data window |
| PostgreSQL for primary storage | ACID guarantees, relational model | Slower than NoSQL |

## Request Flow Diagrams

### Cached Read (Cache Hit)
```
┌──────┐       ┌─────────────┐       ┌───────┐
│Client│       │  Service    │       │ Redis │
└──┬───┘       └──────┬──────┘       └───┬───┘
   │                  │                  │
   │  GET /products/1 │                  │
   ├─────────────────►│                  │
   │                  │                  │
   │                  │  Check cache     │
   │                  ├─────────────────►│
   │                  │                  │
   │                  │ ◄────────────────┤
   │                  │  (HIT: cached    │
   │                  │   product data)  │
   │                  │                  │
   │ ◄─────────────────┤                  │
   │  200 OK          │                  │
   │  (5-15ms)        │                  │
   │                  │                  │
```

### Uncached Read (Cache Miss)
```
┌──────┐   ┌─────────────┐   ┌───────┐   ┌──────────┐
│Client│   │  Service    │   │ Redis │   │PostgreSQL│
└──┬───┘   └──────┬──────┘   └───┬───┘   └────┬─────┘
   │              │              │            │
   │ GET /products/1             │            │
   ├─────────────►│              │            │
   │              │              │            │
   │              │ Check cache  │            │
   │              ├─────────────►│            │
   │              │              │            │
   │              │◄─────────────┤            │
   │              │  (MISS)      │            │
   │              │              │            │
   │              │  Query DB    │            │
   │              ├──────────────────────────►│
   │              │              │            │
   │              │◄──────────────────────────┤
   │              │  (product data)           │
   │              │              │            │
   │              │  Put in cache│            │
   │              ├─────────────►│            │
   │              │              │            │
   │◄─────────────┤              │            │
   │  200 OK      │              │            │
   │  (50-100ms)  │              │            │
   │              │              │            │
```

### Write with Cache Invalidation
```
┌──────┐   ┌─────────────┐   ┌───────┐   ┌──────────┐
│Client│   │  Service    │   │ Redis │   │PostgreSQL│
└──┬───┘   └──────┬──────┘   └───┬───┘   └────┬─────┘
   │              │              │            │
   │ PUT /products/1             │            │
   ├─────────────►│              │            │
   │              │              │            │
   │              │  Update DB   │            │
   │              ├──────────────────────────►│
   │              │              │            │
   │              │◄──────────────────────────┤
   │              │  (updated)   │            │
   │              │              │            │
   │              │  Evict cache │            │
   │              ├─────────────►│            │
   │              │              │            │
   │◄─────────────┤              │            │
   │  200 OK      │              │            │
   │              │              │            │
```

## Component Architecture

### Controller Layer
```
ProductController
├─ GET    /api/products/{id}        → getProduct()
├─ GET    /api/products              → getAllProducts()
├─ POST   /api/products              → createProduct()
├─ PUT    /api/products/{id}        → updateProduct()
└─ DELETE /api/products/{id}        → deleteProduct()

RateLimitController
├─ GET /api/rate-limit/fixed-window/{userId}
├─ GET /api/rate-limit/sliding-window/{userId}
└─ GET /api/rate-limit/token-bucket/{userId}

LeaderboardController
├─ POST /api/leaderboard/{name}/score
├─ GET  /api/leaderboard/{name}/top
└─ GET  /api/leaderboard/{name}/rank/{playerId}

SessionController
├─ POST   /api/sessions
├─ GET    /api/sessions/{id}
└─ DELETE /api/sessions/{id}

DistributedLockController
├─ POST /api/locks/{key}/execute
└─ GET  /api/locks/{key}/status
```

### Service Layer
```
ProductService
├─ @Cacheable getProduct()
├─ @CachePut createProduct()
├─ @CacheEvict updateProduct()
└─ @CacheEvict deleteProduct()

RateLimitService
├─ isAllowedFixedWindow()
├─ isAllowedSlidingWindow()
└─ isAllowedTokenBucket()

LeaderboardService
├─ setScore()
├─ incrementScore()
├─ getTopPlayers()
└─ getRank()

SessionService
├─ createSession()
├─ getSession()
├─ setAttribute()
└─ invalidateSession()

DistributedLockService
├─ executeWithLock()
├─ tryLock()
└─ unlock()
```

### Repository Layer
```
ProductRepository extends JpaRepository
CategoryRepository extends JpaRepository
UserRepository extends JpaRepository
```

### Configuration Layer
```
RedisConfig
├─ redisTemplate()
└─ cacheManager()

RedissonConfig
└─ redissonClient()

CacheMetricsConfig
└─ bindCacheMetrics()
```

## Data Flow

### Product Catalog Flow
1. **Read Path (Cache Hit)**
   - Client → Controller → Service
   - Service checks Redis cache
   - Cache hit: Return cached data (5-15ms)

2. **Read Path (Cache Miss)**
   - Client → Controller → Service
   - Service checks Redis cache
   - Cache miss: Query PostgreSQL (50-100ms)
   - Populate Redis cache (TTL: 5 min)
   - Return data

3. **Write Path**
   - Client → Controller → Service
   - Service updates PostgreSQL
   - Service invalidates Redis cache
   - Return success

### Rate Limiting Flow
1. Client makes request
2. Controller calls RateLimitService
3. RateLimitService checks Redis counter
4. If limit exceeded: Return 429 Too Many Requests
5. If allowed: Increment counter, process request

### Leaderboard Flow
1. Client updates score
2. Controller calls LeaderboardService
3. LeaderboardService updates Redis Sorted Set (ZADD)
4. Score automatically re-ranks in O(log N) time

## Quality Attribute Scenarios

### Performance
**Scenario:** User browses product catalog during peak hours

| Aspect | Target | Actual |
|--------|--------|--------|
| Stimulus | 1000 concurrent users | - |
| Response | Serve catalog pages | - |
| Measure | p95 latency < 50ms | ~15ms (cached) |

**Architecture support:**
- Redis caching (10x speedup)
- Connection pooling (8 connections)
- Indexed database queries

### Scalability
**Scenario:** Traffic increases 10x

| Aspect | Target | Actual |
|--------|--------|--------|
| Stimulus | 10,000 req/s | - |
| Response | Maintain SLA | - |
| Measure | p95 < 100ms | - |

**Architecture support:**
- Stateless application (horizontal scaling)
- Centralized Redis cache (shared across instances)
- Database connection pooling

### Availability
**Scenario:** Redis becomes unavailable

| Aspect | Target | Actual |
|--------|--------|--------|
| Stimulus | Redis crashes | - |
| Response | Degrade gracefully | - |
| Measure | Serve from DB | Yes (fail-open) |

**Architecture support:**
- Cache-aside pattern (DB as source of truth)
- Spring Cache exception handling
- Circuit breaker (can be added)

### Consistency
**Scenario:** Product price updated

| Aspect | Target | Actual |
|--------|--------|--------|
| Stimulus | Admin updates product | - |
| Response | Users see new price | - |
| Measure | Within 5 seconds | Immediate (cache eviction) |

**Architecture support:**
- Event-based cache invalidation
- @CacheEvict on updates
- TTL as safety net (5 min max staleness)

## Deployment Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Load Balancer                       │
└────────────────────┬────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        ▼            ▼            ▼
   ┌────────┐  ┌────────┐  ┌────────┐
   │ App    │  │ App    │  │ App    │
   │Instance│  │Instance│  │Instance│
   │   1    │  │   2    │  │   3    │
   └────┬───┘  └────┬───┘  └────┬───┘
        │           │           │
        └───────────┼───────────┘
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
   ┌─────────┐  ┌─────────┐
   │ Redis   │  │PostgreSQL│
   │(Cluster)│  │(Primary) │
   │         │  │          │
   │ Master  │  │          │
   │ Replica │  │          │
   │ Sentinel│  │          │
   └─────────┘  └─────────┘
```

## Monitoring and Observability

### Metrics (Prometheus)
```
# Cache Performance
cache_hits_total{cache="products"}
cache_misses_total{cache="products"}
cache_evictions_total{cache="products"}

# Application Performance
http_server_requests_seconds{uri="/api/products/{id}"}
redis_commands_duration_seconds
db_query_duration_seconds

# Infrastructure
redis_memory_used_bytes
redis_connected_clients
db_connections_active
```

### Health Checks
```
GET /actuator/health

{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

## Evolution Roadmap

### Short-term (0-3 months)
- [ ] Add circuit breaker for Redis
- [ ] Implement cache warming on startup
- [ ] Add Grafana dashboards

### Medium-term (3-6 months)
- [ ] Redis Cluster for horizontal scaling
- [ ] Read replicas for PostgreSQL
- [ ] Pub/Sub for cache invalidation across instances

### Long-term (6-12 months)
- [ ] Extract to microservices architecture
- [ ] Event-driven architecture with Kafka
- [ ] Multi-region deployment

## References

- [C4 Model](https://c4model.com/) - Architecture diagram approach
- [Spring Boot Best Practices](https://spring.io/guides)
- [Redis Architecture Patterns](https://redis.io/docs/reference/patterns/)
- [The Twelve-Factor App](https://12factor.net/)
