# L7: Redis Caching Patterns

## Overview

This lesson demonstrates **advanced Redis usage patterns** in a production-grade Spring Boot application. You'll learn how to implement various caching strategies, distributed systems patterns, and real-time features using Redis as your data store.

## Learning Objectives

By completing this lesson, you will:

1. Understand and implement various **caching patterns** (Cache-Aside, Write-Through, Write-Behind)
2. Implement **rate limiting** using multiple algorithms (Fixed Window, Sliding Window, Token Bucket)
3. Use **distributed locks** for coordinating across multiple application instances
4. Build **real-time leaderboards** using Redis Sorted Sets
5. Manage **user sessions** with Redis for scalability
6. Apply **cache invalidation strategies** to maintain data consistency
7. Monitor and optimize **cache performance** using metrics
8. Understand **Redis data structures** and when to use each one

## What You'll Build

A **Product Catalog Service** demonstrating:

- **Cache-Aside Pattern**: Product catalog with intelligent caching
- **Rate Limiting**: API protection with 3 different algorithms
- **Distributed Locks**: Safe concurrent operations across instances
- **Leaderboards**: Real-time product rankings by views
- **Session Management**: Scalable user session storage
- **Metrics & Monitoring**: Prometheus metrics for cache performance

## Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- Basic understanding of Spring Boot
- Basic understanding of REST APIs
- Completed lessons L1-L6 (recommended)

## Project Structure

```
L7-redis-caching-patterns/
├── src/
│   ├── main/
│   │   ├── java/.../redis/
│   │   │   ├── config/          # Redis & Redisson configuration
│   │   │   ├── controller/      # REST API endpoints
│   │   │   ├── service/         # Business logic with caching
│   │   │   ├── repository/      # JPA repositories
│   │   │   ├── model/
│   │   │   │   ├── entity/      # Database entities
│   │   │   │   ├── request/     # Request DTOs
│   │   │   │   ├── response/    # Response DTOs
│   │   │   │   └── enums/       # Enumerations
│   │   │   ├── exception/       # Exception handling
│   │   │   └── RedisCachingPatternsApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-prod.yml
│   │       └── db/changelog/    # Liquibase migrations
│   └── test/                    # Unit & integration tests
├── docs/
│   ├── adr/                     # Architecture Decision Records
│   └── architecture/            # C4 diagrams
├── monitoring/
│   └── prometheus.yml           # Prometheus configuration
├── docker-compose.yml           # Infrastructure services
├── build.gradle
├── README.md                    # This file
├── KEY_CONCEPTS.md             # Conceptual learning
├── EXERCISES.md                # Hands-on exercises
└── LINKS.md                    # Learning resources
```

## Quick Start

### 1. Start Infrastructure

Start PostgreSQL, Redis, Redis Sentinel, Redis Insight, and Prometheus:

```bash
docker-compose up -d
```

Verify all services are healthy:

```bash
docker-compose ps
```

### 2. Build and Run Application

```bash
# Build the application
./gradlew clean build

# Run the application
./gradlew bootRun
```

The application will start on port 8080.

### 3. Verify Setup

**Check application health:**

```bash
curl http://localhost:8080/actuator/health
```

**Access Redis Insight UI:**

Open http://localhost:8001 in your browser to visualize Redis data.

**Access Prometheus:**

Open http://localhost:9090 to view metrics.

## API Endpoints

### Product Caching (Cache-Aside Pattern)

```bash
# Get product (cached)
curl http://localhost:8080/api/products/1

# Get all products (paginated, not cached)
curl http://localhost:8080/api/products?page=0&size=10

# Search products (not cached due to dynamic nature)
curl http://localhost:8080/api/products/search?keyword=headphones

# Get most viewed products (cached with short TTL)
curl http://localhost:8080/api/products/most-viewed?limit=5

# Create product (populates cache)
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "TEST-001",
    "name": "Test Product",
    "description": "A test product",
    "price": 99.99,
    "stockQuantity": 50,
    "categoryId": 1,
    "status": "ACTIVE"
  }'

# Update product (invalidates cache)
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Product Name",
    "price": 149.99
  }'

# Increment view count (bypasses cache)
curl -X POST http://localhost:8080/api/products/1/view

# Clear all product caches
curl -X POST http://localhost:8080/api/products/cache/clear
```

### Rate Limiting

```bash
# Test fixed window rate limiting (10 requests per 60 seconds)
for i in {1..15}; do
  curl http://localhost:8080/api/rate-limit/fixed-window/user123?maxRequests=10&windowSeconds=60
  sleep 1
done

# Test sliding window rate limiting
curl http://localhost:8080/api/rate-limit/sliding-window/user123?maxRequests=5&windowSeconds=30

# Test token bucket rate limiting
curl http://localhost:8080/api/rate-limit/token-bucket/user123?capacity=10&refillRate=2.0&tokensRequired=1

# Reset rate limit for a user
curl -X POST http://localhost:8080/api/rate-limit/reset/user123
```

### Leaderboard (Redis Sorted Sets)

```bash
# Set player score
curl -X POST "http://localhost:8080/api/leaderboard/global/score?playerId=player1&score=1500"

# Increment player score
curl -X POST "http://localhost:8080/api/leaderboard/global/increment?playerId=player1&delta=100"

# Get player score
curl http://localhost:8080/api/leaderboard/global/score/player1

# Get player rank
curl http://localhost:8080/api/leaderboard/global/rank/player1

# Get top 10 players
curl http://localhost:8080/api/leaderboard/global/top?count=10

# Get players around a specific player
curl http://localhost:8080/api/leaderboard/global/around/player1?range=3

# Remove player
curl -X DELETE http://localhost:8080/api/leaderboard/global/player/player1

# Clear leaderboard
curl -X DELETE http://localhost:8080/api/leaderboard/global
```

### Session Management

```bash
# Create session
SESSION_ID=$(curl -X POST "http://localhost:8080/api/sessions?userId=1" \
  -H "Content-Type: application/json" \
  -d '{"cart": [], "preferences": {}}' | tr -d '"')

# Get session
curl http://localhost:8080/api/sessions/$SESSION_ID

# Set session attribute
curl -X PUT http://localhost:8080/api/sessions/$SESSION_ID/attributes/cartItem \
  -H "Content-Type: application/json" \
  -d '"Product ABC"'

# Get session attribute
curl http://localhost:8080/api/sessions/$SESSION_ID/attributes/cartItem

# Get all active sessions for a user
curl http://localhost:8080/api/sessions/user/1

# Invalidate session
curl -X DELETE http://localhost:8080/api/sessions/$SESSION_ID

# Invalidate all user sessions
curl -X DELETE http://localhost:8080/api/sessions/user/1
```

### Distributed Locks

```bash
# Execute operation with lock (will wait up to 5 seconds)
curl -X POST "http://localhost:8080/api/locks/inventory-update/execute?waitTime=5&leaseTime=10"

# Try to acquire lock (non-blocking)
curl -X POST "http://localhost:8080/api/locks/order-processing/try?leaseTime=30"

# Check lock status
curl http://localhost:8080/api/locks/order-processing/status

# Release lock
curl -X POST http://localhost:8080/api/locks/order-processing/unlock
```

## Observing Cache Behavior

### 1. Monitor Cache Metrics

View cache statistics in Prometheus:

```
http://localhost:9090/graph
```

Example queries:

```promql
# Cache hit ratio
cache_gets{name="products",result="hit"} / cache_gets{name="products"}

# Cache size
cache_size{name="products"}

# Cache evictions
rate(cache_evictions_total{name="products"}[5m])
```

### 2. View Data in Redis Insight

1. Open http://localhost:8001
2. Connect to Redis (localhost:6379)
3. Browse keys to see:
   - `products::*` - Cached products
   - `rate_limit:*` - Rate limiting counters
   - `leaderboard:*` - Sorted sets for rankings
   - `session:*` - User sessions
   - `lock:*` - Distributed locks

### 3. Test Cache Effectiveness

```bash
# First request (cache miss)
time curl http://localhost:8080/api/products/1

# Second request (cache hit - should be faster)
time curl http://localhost:8080/api/products/1

# Update product (cache invalidation)
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"price": 199.99}'

# Next request (cache miss again)
time curl http://localhost:8080/api/products/1
```

## Performance Benchmarking

### Database vs Cache Response Time

Use Apache Bench or similar tools:

```bash
# Without cache (clear cache first)
curl -X POST http://localhost:8080/api/products/cache/clear
ab -n 1000 -c 10 http://localhost:8080/api/products/1

# With warm cache
ab -n 1000 -c 10 http://localhost:8080/api/products/1
```

Expected results:
- **Without cache**: 50-100ms per request
- **With cache**: 5-15ms per request
- **Improvement**: 5-10x faster response times

### Rate Limiting Performance

Test rate limit enforcement:

```bash
# Send 100 requests rapidly
ab -n 100 -c 10 "http://localhost:8080/api/rate-limit/fixed-window/user123?maxRequests=50&windowSeconds=60"
```

Monitor:
- Requests allowed before rate limit kicks in
- 429 Too Many Requests responses after limit exceeded
- Rate limit reset behavior

## Common Use Cases Demonstrated

### 1. Read-Heavy Workloads
- **Pattern**: Cache-Aside
- **Example**: Product catalog browsing
- **Benefit**: 10x faster reads, reduced database load

### 2. API Protection
- **Pattern**: Rate Limiting
- **Example**: Public API endpoints
- **Benefit**: Prevent abuse, ensure fair usage

### 3. Distributed Coordination
- **Pattern**: Distributed Locks
- **Example**: Inventory updates, order processing
- **Benefit**: Prevent race conditions across instances

### 4. Real-Time Rankings
- **Pattern**: Sorted Sets
- **Example**: Product leaderboards, gaming scores
- **Benefit**: O(log N) updates and queries

### 5. Scalable Sessions
- **Pattern**: Centralized Session Store
- **Example**: User shopping carts, preferences
- **Benefit**: Stateless application servers

## Troubleshooting

### Redis Connection Issues

```bash
# Check Redis is running
docker ps | grep redis

# Test Redis connection
docker exec -it redis-patterns-redis redis-cli ping

# View Redis logs
docker logs redis-patterns-redis
```

### Database Connection Issues

```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Test connection
docker exec -it redis-patterns-postgres psql -U admin -d redis_patterns_db -c "SELECT 1"

# View PostgreSQL logs
docker logs redis-patterns-postgres
```

### Application Logs

```bash
# Tail application logs
./gradlew bootRun --console=plain

# Check cache behavior
grep "Fetching product from database" logs/application.log
```

### Clear All Redis Data

```bash
# Connect to Redis CLI
docker exec -it redis-patterns-redis redis-cli

# Clear all keys
> FLUSHALL

# Verify
> DBSIZE
```

## Next Steps

1. Complete the [EXERCISES.md](EXERCISES.md) for hands-on practice
2. Read [KEY_CONCEPTS.md](KEY_CONCEPTS.md) for deep conceptual understanding
3. Review [Architecture Decision Records](docs/adr/) to understand design choices
4. Explore [LINKS.md](LINKS.md) for additional learning resources
5. Experiment with different cache TTLs and eviction policies
6. Try implementing additional patterns (Pub/Sub, Bloom Filters, HyperLogLog)

## Key Takeaways

1. **Caching is powerful but adds complexity** - Use only when needed
2. **Cache invalidation is hard** - Plan your invalidation strategy upfront
3. **Monitor cache hit ratio** - Below 70% suggests cache isn't effective
4. **Choose the right Redis data structure** - Each has specific use cases
5. **Consider consistency requirements** - Caching trades consistency for performance
6. **Rate limiting protects your system** - Multiple algorithms for different needs
7. **Distributed locks enable coordination** - Essential for multi-instance deployments

## Additional Resources

- [Redis Official Documentation](https://redis.io/docs/)
- [Spring Data Redis Documentation](https://spring.io/projects/spring-data-redis)
- [Redisson Documentation](https://github.com/redisson/redisson/wiki)
- [Cache Patterns Best Practices](https://docs.aws.amazon.com/whitepapers/latest/database-caching-strategies-using-redis/welcome.html)

## License

This course material is provided for educational purposes.

---

**Happy Caching!** If you encounter issues, check the [troubleshooting section](#troubleshooting) or review the logs.
