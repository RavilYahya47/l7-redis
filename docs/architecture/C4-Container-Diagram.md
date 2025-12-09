# C4 Container Diagram: Redis Caching Patterns

## Overview

The Container diagram zooms into the Redis Caching Patterns system, showing the high-level technical building blocks (containers) and how they interact.

## Diagram

```
                        ┌──────────────────┐
                        │   Web Browser    │
                        │  (JavaScript)    │
                        └────────┬─────────┘
                                 │
                                 │ HTTPS
                                 │ REST/JSON
                                 │
                                 ▼
┌────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                 │
│                    (Java 17, Port 8080)                    │
│                                                             │
│  ┌───────────────────────────────────────────────────┐    │
│  │           REST API Layer                          │    │
│  │  - ProductController                               │    │
│  │  - RateLimitController                            │    │
│  │  - LeaderboardController                          │    │
│  │  - SessionController                              │    │
│  │  - DistributedLockController                      │    │
│  └─────────────────┬─────────────────────────────────┘    │
│                    │                                        │
│                    ▼                                        │
│  ┌───────────────────────────────────────────────────┐    │
│  │          Service Layer                            │    │
│  │  - ProductService (@Cacheable)                    │    │
│  │  - RateLimitService                               │    │
│  │  - LeaderboardService                             │    │
│  │  - SessionService                                 │    │
│  │  - DistributedLockService                         │    │
│  └──────┬──────────────────────┬─────────────────────┘    │
│         │                      │                           │
│         │                      │                           │
│         ▼                      ▼                           │
│  ┌─────────────────┐   ┌────────────────────────┐        │
│  │  JPA Repository │   │  Redis Operations      │        │
│  │  Layer          │   │  (RedisTemplate,       │        │
│  │  - ProductRepo  │   │   RedissonClient)      │        │
│  │  - CategoryRepo │   │                        │        │
│  │  - UserRepo     │   │                        │        │
│  └─────────────────┘   └────────────────────────┘        │
└───────┬─────────────────────────┬──────────────────────────┘
        │                         │
        │ JDBC                    │ TCP/6379
        │                         │ Redis Protocol
        │                         │
        ▼                         ▼
┌──────────────────┐     ┌──────────────────────────┐
│   PostgreSQL     │     │        Redis             │
│   Database       │     │   In-Memory Store        │
│   (Port 5432)    │     │   (Port 6379)            │
│                  │     │                          │
│  - Products      │     │  - Product Cache         │
│  - Categories    │     │  - Sessions              │
│  - Users         │     │  - Rate Limit Counters   │
│                  │     │  - Leaderboards (ZSet)   │
│  Persistent      │     │  - Distributed Locks     │
│  Storage         │     │                          │
│                  │     │  In-Memory + AOF/RDB     │
└──────────────────┘     └──────────────────────────┘
         │                         │
         │                         │
         ▼                         ▼
┌──────────────────┐     ┌──────────────────────────┐
│  Redis Insight   │     │     Prometheus           │
│  (Port 8001)     │     │     (Port 9090)          │
│                  │     │                          │
│  GUI for Redis   │     │  Scrapes /actuator/      │
│  data inspection │     │  prometheus              │
└──────────────────┘     │                          │
                         │  Stores metrics          │
                         └──────────────────────────┘
```

## Containers

### 1. Spring Boot Application
**Technology:** Java 17, Spring Boot 3.2, Gradle
**Port:** 8080
**Responsibilities:**
- Handle HTTP REST API requests
- Implement business logic
- Manage caching with Spring Cache
- Coordinate with Redis and PostgreSQL

**Components:**
- **REST API Layer**: Controllers exposing HTTP endpoints
- **Service Layer**: Business logic with caching annotations
- **Repository Layer**: Data access (JPA for DB, RedisTemplate for cache)

**Key Libraries:**
- Spring Web (REST APIs)
- Spring Data JPA (Database access)
- Spring Data Redis (Redis integration)
- Spring Cache (Caching abstraction)
- Redisson (Advanced Redis features)
- Liquibase (Database migrations)
- Micrometer (Metrics)

---

### 2. PostgreSQL Database
**Technology:** PostgreSQL 16
**Port:** 5432
**Responsibilities:**
- Store persistent data (source of truth)
- Support transactional operations (ACID)
- Provide relational querying capabilities

**Schema:**
```sql
products
├─ id (PK)
├─ sku (unique)
├─ name
├─ price
├─ stock_quantity
├─ category_id (FK)
└─ status

categories
├─ id (PK)
├─ name
├─ slug (unique)
└─ parent_id (FK, self-reference)

users
├─ id (PK)
├─ username (unique)
├─ email (unique)
├─ password_hash
└─ role
```

**Interactions:**
- Application reads on cache miss
- Application writes on updates
- Application uses connection pooling (Hikari, max 10 connections)

---

### 3. Redis In-Memory Store
**Technology:** Redis 7
**Port:** 6379
**Responsibilities:**
- Cache frequently accessed data
- Store session data
- Manage rate limit counters
- Host real-time leaderboards
- Provide distributed locks

**Data Structures Used:**
```
Strings:
- products::1 → JSON serialized product
- rate_limit:fixed:user123 → counter
- session:abc-123 → JSON serialized session

Sorted Sets (ZSet):
- leaderboard:global → [(player1, 1500), (player2, 2000), ...]

Sets:
- active_sessions:user123 → {session1, session2, session3}

Hashes: (alternative structure for sessions)
- session:abc-123
  ├─ userId: "1"
  ├─ cart: "[...]"
  └─ lastAccess: "2024-01-01T12:00:00"
```

**Configuration:**
- Max memory: 256MB
- Eviction policy: allkeys-lru
- Persistence: AOF (Append-Only File)
- Replication: Sentinel for HA (in production)

**Interactions:**
- Application reads/writes via Lettuce client
- Application uses Redisson for distributed locks
- Prometheus scrapes Redis metrics (via exporter)

---

### 4. Redis Insight
**Technology:** Redis Insight (Web UI)
**Port:** 8001
**Responsibilities:**
- Provide GUI for Redis data exploration
- Visualize data structures
- Execute Redis commands
- Debug caching behavior

**Use Cases:**
- Inspect cached products
- View leaderboard rankings
- Check rate limit counters
- Verify session data
- Debug cache invalidation

---

### 5. Prometheus
**Technology:** Prometheus
**Port:** 9090
**Responsibilities:**
- Scrape application metrics
- Store time-series data
- Enable querying for dashboards
- Trigger alerts

**Metrics Collected:**
```
# Cache Metrics
cache_gets_total{cache="products", result="hit"}
cache_gets_total{cache="products", result="miss"}
cache_evictions_total{cache="products"}

# HTTP Metrics
http_server_requests_seconds{uri="/api/products/{id}", status="200"}

# Redis Metrics
redis_commands_duration_seconds{command="GET"}
redis_memory_used_bytes

# JVM Metrics
jvm_memory_used_bytes{area="heap"}
jvm_threads_live
```

**Scrape Configuration:**
```yaml
scrape_configs:
  - job_name: 'redis-patterns-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
```

---

## Data Flow

### Read Path (Cached)
```
1. Browser → Spring Boot Application
   GET /api/products/1

2. Spring Boot → Redis
   Check cache: GET "products::1"

3. Redis → Spring Boot
   Return cached JSON (5-15ms)

4. Spring Boot → Browser
   200 OK {product data}
```

### Read Path (Uncached)
```
1. Browser → Spring Boot Application
   GET /api/products/1

2. Spring Boot → Redis
   Check cache: GET "products::1"

3. Redis → Spring Boot
   NULL (cache miss)

4. Spring Boot → PostgreSQL
   SELECT * FROM products WHERE id = 1

5. PostgreSQL → Spring Boot
   Product row (50-100ms)

6. Spring Boot → Redis
   SET "products::1" {json} EX 300

7. Spring Boot → Browser
   200 OK {product data}
```

### Write Path
```
1. Browser → Spring Boot Application
   PUT /api/products/1 {updated data}

2. Spring Boot → PostgreSQL
   UPDATE products SET price = 199.99 WHERE id = 1

3. Spring Boot → Redis
   DEL "products::1" (cache invalidation)

4. Spring Boot → Browser
   200 OK {updated product}
```

### Rate Limiting
```
1. Browser → Spring Boot Application
   GET /api/rate-limit/fixed-window/user123

2. Spring Boot → Redis
   INCR rate_limit:fixed:user123

3. Redis → Spring Boot
   Returns count (e.g., 5)

4. Spring Boot evaluates:
   IF count > max (10): Return 429 Too Many Requests
   ELSE: Process request, return 200 OK
```

### Leaderboard Update
```
1. Browser → Spring Boot Application
   POST /api/leaderboard/global/score?playerId=alice&score=1500

2. Spring Boot → Redis
   ZADD leaderboard:global 1500 alice

3. Redis automatically maintains sorted order

4. Spring Boot → Browser
   200 OK
```

## Technology Choices

### Why Spring Boot?
- Mature ecosystem for microservices
- Built-in caching abstraction
- Excellent Redis integration
- Auto-configuration reduces boilerplate
- Production-ready features (metrics, health checks)

### Why Redis over Memcached?
- Advanced data structures (Sorted Sets for leaderboards)
- Persistence options (survive restarts)
- Distributed locks (coordination)
- Richer feature set

### Why PostgreSQL over NoSQL?
- Relational data model fits product catalog
- ACID transactions for consistency
- SQL for complex queries
- Team expertise

### Why Lettuce over Jedis?
- Non-blocking I/O (better concurrency)
- Thread-safe (fewer connections)
- Spring Boot default
- Reactive support (future-proofing)

## Deployment

### Development (Docker Compose)
```yaml
services:
  postgres:
    image: public.ecr.aws/docker/library/postgres:16-alpine
    ports: ["5432:5432"]

  redis:
    image: public.ecr.aws/docker/library/redis:7-alpine
    ports: ["6379:6379"]

  redis-insight:
    image: public.ecr.aws/redis/redisinsight:latest
    ports: ["8001:8001"]

  prometheus:
    image: public.ecr.aws/ubuntu/prometheus:latest
    ports: ["9090:9090"]
```

Application runs locally via `./gradlew bootRun`.

### Production (Conceptual)
- **Application**: Kubernetes pods (3+ replicas)
- **Redis**: Redis Cluster or AWS ElastiCache
- **PostgreSQL**: Managed service (AWS RDS, Azure Database)
- **Monitoring**: Managed Prometheus/Grafana

## Scalability

### Horizontal Scaling
```
Load Balancer
├─ App Instance 1 ────┐
├─ App Instance 2 ────┼── Shared Redis
├─ App Instance 3 ────┘
└─ App Instance N

All instances share:
- Redis cache (centralized)
- PostgreSQL database (connection pooled)
```

### Vertical Scaling
- Increase Redis memory (256MB → 1GB → 4GB)
- Increase PostgreSQL resources (CPU, RAM)
- Increase app instance resources

## Next Steps

- See [C4 Component Diagram](C4-Component-Diagram.md) for internal code structure
- See [Architecture Overview](ARCHITECTURE_OVERVIEW.md) for detailed design decisions
- See [ADRs](../adr/) for architecture decision rationale
