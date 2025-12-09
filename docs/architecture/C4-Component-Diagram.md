# C4 Component Diagram: Spring Boot Application

## Overview

The Component diagram zooms into the Spring Boot Application container, showing the internal components and their interactions.

## Package Structure

```
com.course.lesson7.redis
├── RedisCachingPatternsApplication.java    # Main class
├── config/                                   # Configuration
│   ├── RedisConfig.java
│   ├── RedissonConfig.java
│   └── CacheMetricsConfig.java
├── controller/                               # REST API
│   ├── ProductController.java
│   ├── RateLimitController.java
│   ├── LeaderboardController.java
│   ├── SessionController.java
│   └── DistributedLockController.java
├── service/                                  # Business Logic
│   ├── ProductService.java
│   ├── RateLimitService.java
│   ├── LeaderboardService.java
│   ├── SessionService.java
│   └── DistributedLockService.java
├── repository/                               # Data Access
│   ├── ProductRepository.java
│   ├── CategoryRepository.java
│   └── UserRepository.java
├── model/                                    # Data Models
│   ├── entity/
│   │   ├── Product.java
│   │   ├── Category.java
│   │   └── User.java
│   ├── request/
│   │   ├── CreateProductRequest.java
│   │   └── UpdateProductRequest.java
│   ├── response/
│   │   ├── ProductResponse.java
│   │   └── UserResponse.java
│   └── enums/
│       ├── ProductStatus.java
│       ├── UserRole.java
│       └── UserStatus.java
└── exception/                                # Exception Handling
    ├── ProductNotFoundException.java
    └── GlobalExceptionHandler.java
```

## Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Controller Layer (REST API)                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────┐  ┌───────────────────┐  ┌──────────────┐│
│  │ Product          │  │ RateLimit         │  │ Leaderboard  ││
│  │ Controller       │  │ Controller        │  │ Controller   ││
│  │                  │  │                   │  │              ││
│  │ @RestController  │  │ @RestController   │  │ @Rest...     ││
│  │ @RequestMapping  │  │                   │  │              ││
│  └────────┬─────────┘  └────────┬──────────┘  └──────┬───────┘│
│           │                     │                     │        │
│           │  Calls              │                     │        │
└───────────┼─────────────────────┼─────────────────────┼────────┘
            │                     │                     │
            ▼                     ▼                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Service Layer (Business Logic)               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ ProductService                                            │  │
│  │                                                           │  │
│  │ @Service                                                  │  │
│  │ @Transactional                                            │  │
│  │                                                           │  │
│  │ Methods:                                                  │  │
│  │  + @Cacheable getProduct(Long id)                        │  │
│  │  + @CachePut createProduct(CreateProductRequest)         │  │
│  │  + @CacheEvict updateProduct(Long, UpdateProductRequest) │  │
│  │  + @CacheEvict deleteProduct(Long id)                    │  │
│  │  + incrementViewCount(Long id)                           │  │
│  │  + @Cacheable getMostViewedProducts(int limit)           │  │
│  └───────────┬──────────────────────────┬───────────────────┘  │
│              │                          │                       │
│  ┌───────────┴──────────┐  ┌───────────┴───────────┐          │
│  │ RateLimitService     │  │ LeaderboardService    │          │
│  │                      │  │                       │          │
│  │ @Service             │  │ @Service              │          │
│  │                      │  │                       │          │
│  │ Methods:             │  │ Methods:              │          │
│  │  + isAllowedFixed... │  │  + setScore(...)      │          │
│  │  + isAllowedSliding  │  │  + incrementScore(..) │          │
│  │  + isAllowedToken... │  │  + getTopPlayers(...) │          │
│  │  + reset(String key) │  │  + getRank(...)       │          │
│  └──────────────────────┘  └───────────────────────┘          │
│                                                                  │
└──────────────┬────────────────────────┬──────────────────────────┘
               │                        │
               ▼                        ▼
┌──────────────────────────┐   ┌──────────────────────────────┐
│  Repository Layer        │   │  Redis Operations            │
│                          │   │                              │
│  ┌────────────────────┐ │   │  ┌────────────────────────┐ │
│  │ ProductRepository  │ │   │  │ RedisTemplate<K,V>     │ │
│  │                    │ │   │  │                        │ │
│  │ extends            │ │   │  │ Methods:               │ │
│  │ JpaRepository      │ │   │  │  + opsForValue()       │ │
│  │                    │ │   │  │  + opsForList()        │ │
│  │ Methods:           │ │   │  │  + opsForSet()         │ │
│  │  + findBySku(...)  │ │   │  │  + opsForZSet()        │ │
│  │  + findByStatus... │ │   │  │  + opsForHash()        │ │
│  │  + searchProducts  │ │   │  └────────────────────────┘ │
│  └────────────────────┘ │   │                              │
│                          │   │  ┌────────────────────────┐ │
│  ┌────────────────────┐ │   │  │ RedissonClient         │ │
│  │ CategoryRepository │ │   │  │                        │ │
│  │                    │ │   │  │ Methods:               │ │
│  │ extends            │ │   │  │  + getLock(key)        │ │
│  │ JpaRepository      │ │   │  │  + getFairLock(key)    │ │
│  └────────────────────┘ │   │  │  + getSemaphore(key)   │ │
│                          │   │  └────────────────────────┘ │
└──────────────┬───────────┘   └────────────┬─────────────────┘
               │                            │
               │                            │
               ▼                            ▼
        ┌─────────────┐            ┌──────────────┐
        │ PostgreSQL  │            │    Redis     │
        └─────────────┘            └──────────────┘
```

## Component Descriptions

### Controller Layer

#### ProductController
**Responsibility:** Handle HTTP requests for product operations

**Endpoints:**
```java
GET    /api/products/{id}              → getProduct()
GET    /api/products                    → getAllProducts()
POST   /api/products                    → createProduct()
PUT    /api/products/{id}              → updateProduct()
DELETE /api/products/{id}              → deleteProduct()
GET    /api/products/search?keyword=... → searchProducts()
GET    /api/products/most-viewed        → getMostViewedProducts()
POST   /api/products/{id}/view          → incrementViewCount()
```

**Dependencies:**
- ProductService (business logic)

**Example:**
```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        ProductResponse product = productService.getProduct(id);
        return ResponseEntity.ok(product);
    }
}
```

---

#### RateLimitController
**Responsibility:** Demonstrate rate limiting algorithms

**Endpoints:**
```java
GET /api/rate-limit/fixed-window/{userId}   → testFixedWindow()
GET /api/rate-limit/sliding-window/{userId} → testSlidingWindow()
GET /api/rate-limit/token-bucket/{userId}   → testTokenBucket()
POST /api/rate-limit/reset/{userId}         → resetRateLimit()
```

**Dependencies:**
- RateLimitService

---

#### LeaderboardController
**Responsibility:** Manage real-time leaderboards

**Endpoints:**
```java
POST /api/leaderboard/{name}/score           → setScore()
POST /api/leaderboard/{name}/increment       → incrementScore()
GET  /api/leaderboard/{name}/top             → getTopPlayers()
GET  /api/leaderboard/{name}/rank/{playerId} → getRank()
```

**Dependencies:**
- LeaderboardService

---

### Service Layer

#### ProductService
**Responsibility:** Product business logic with caching

**Key Methods:**
```java
@Cacheable(value = "products", key = "#id")
public ProductResponse getProduct(Long id)

@CachePut(value = "products", key = "#result.id")
public ProductResponse createProduct(CreateProductRequest request)

@CacheEvict(value = "products", key = "#id")
public ProductResponse updateProduct(Long id, UpdateProductRequest request)

@CacheEvict(value = "products", key = "#id")
public void deleteProduct(Long id)

@Cacheable(value = "analytics", key = "'most-viewed'")
public List<ProductResponse> getMostViewedProducts(int limit)
```

**Caching Strategy:**
- **Read**: Cache-Aside (@Cacheable)
- **Write**: Write-Through + Invalidate (@CachePut, @CacheEvict)
- **TTL**: 5 minutes for products, 2 minutes for analytics

**Dependencies:**
- ProductRepository (database access)
- CategoryRepository (lookup categories)
- Spring Cache (caching abstraction)

---

#### RateLimitService
**Responsibility:** Implement rate limiting algorithms

**Algorithms:**
1. **Fixed Window**: Simple counter with TTL
2. **Sliding Window**: Sorted set with timestamps
3. **Token Bucket**: Refill tokens over time

**Key Methods:**
```java
public boolean isAllowedFixedWindow(String key, int maxRequests, long windowSeconds)
public boolean isAllowedSlidingWindow(String key, int maxRequests, long windowSeconds)
public boolean isAllowedTokenBucket(String key, int capacity, double refillRate, int tokensRequired)
```

**Dependencies:**
- RedisTemplate (counter operations)

---

#### LeaderboardService
**Responsibility:** Manage ranked lists using Sorted Sets

**Data Structure:**
```
Redis ZSet: leaderboard:{name}
Members: playerIds
Scores: player scores (doubles)
```

**Key Methods:**
```java
public void setScore(String leaderboardName, String playerId, double score)
public Double incrementScore(String leaderboardName, String playerId, double delta)
public List<LeaderboardEntry> getTopPlayers(String leaderboardName, int count)
public Long getRank(String leaderboardName, String playerId)
```

**Operations:**
- `ZADD` - Set/update score (O(log N))
- `ZINCRBY` - Increment score (O(log N))
- `ZREVRANGE` - Get top N (O(log N + M))
- `ZREVRANK` - Get player rank (O(log N))

**Dependencies:**
- RedisTemplate (sorted set operations)

---

#### DistributedLockService
**Responsibility:** Coordinate across multiple instances

**Key Methods:**
```java
public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime,
                             TimeUnit unit, Supplier<T> operation)

public boolean tryLock(String lockKey, long leaseTime, TimeUnit unit)
public void unlock(String lockKey)
public boolean isLocked(String lockKey)
```

**Lock Types:**
- **Mutex**: Exclusive lock (one holder)
- **Fair Lock**: FIFO order acquisition
- **Read/Write Lock**: Multiple readers or one writer

**Dependencies:**
- RedissonClient (distributed lock implementation)

---

#### SessionService
**Responsibility:** Manage user sessions in Redis

**Session Data:**
```java
public class SessionData {
    private String sessionId;
    private Long userId;
    private Map<String, Object> attributes;  // Cart, preferences, etc.
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
}
```

**Key Methods:**
```java
public String createSession(Long userId, Map<String, Object> attributes)
public SessionData getSession(String sessionId)
public void setAttribute(String sessionId, String key, Object value)
public void invalidateSession(String sessionId)
```

**TTL Strategy:**
- 30 minutes sliding expiration (reset on access)

**Dependencies:**
- RedisTemplate (session storage)

---

### Repository Layer

#### ProductRepository
**Responsibility:** Database access for products

**Interface:**
```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    List<Product> findByStatus(ProductStatus status);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE ...")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);
}
```

**Database Operations:**
- JPA for CRUD
- Custom queries for complex searches
- Pagination support

---

### Configuration Layer

#### RedisConfig
**Responsibility:** Configure Redis connection and caching

**Beans:**
```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory)

@Bean
public CacheManager cacheManager()
```

**Cache Configuration:**
```java
Map<String, RedisCacheConfiguration> configs = new HashMap<>();
configs.put("products", defaultConfig.entryTtl(Duration.ofMinutes(5)));
configs.put("users", defaultConfig.entryTtl(Duration.ofMinutes(15)));
configs.put("sessions", defaultConfig.entryTtl(Duration.ofMinutes(30)));
configs.put("analytics", defaultConfig.entryTtl(Duration.ofMinutes(2)));
```

---

#### RedissonConfig
**Responsibility:** Configure Redisson for advanced features

**Bean:**
```java
@Bean
public RedissonClient redissonClient() {
    Config config = new Config();
    config.useSingleServer()
        .setAddress("redis://" + redisHost + ":" + redisPort)
        .setConnectionPoolSize(50)
        .setRetryAttempts(3);
    return Redisson.create(config);
}
```

---

### Exception Handling

#### GlobalExceptionHandler
**Responsibility:** Centralized exception handling

**Handlers:**
```java
@ExceptionHandler(ProductNotFoundException.class)
public ResponseEntity<ErrorResponse> handleProductNotFound(...)

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ValidationErrorResponse> handleValidationErrors(...)

@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericException(...)
```

**Error Response:**
```json
{
  "status": 404,
  "message": "Product not found: 123",
  "timestamp": "2024-01-01T12:00:00"
}
```

---

## Data Flow Through Components

### Read Request (Cache Hit)
```
1. Client → ProductController.getProduct(1)
2. ProductController → ProductService.getProduct(1)
3. ProductService → Spring Cache (checks @Cacheable)
4. Spring Cache → Redis (GET "products::1")
5. Redis → Spring Cache → ProductService → ProductController
6. ProductController → Client (200 OK)

Total: 5-15ms
```

### Read Request (Cache Miss)
```
1. Client → ProductController.getProduct(1)
2. ProductController → ProductService.getProduct(1)
3. ProductService → Spring Cache (checks @Cacheable)
4. Spring Cache → Redis (GET "products::1" → NULL)
5. ProductService → ProductRepository.findById(1)
6. ProductRepository → PostgreSQL (SELECT * FROM products WHERE id = 1)
7. PostgreSQL → ProductRepository → ProductService
8. ProductService → Spring Cache → Redis (SET "products::1" {json} EX 300)
9. ProductService → ProductController → Client (200 OK)

Total: 50-100ms
```

### Write Request
```
1. Client → ProductController.updateProduct(1, request)
2. ProductController → ProductService.updateProduct(1, request)
3. ProductService → ProductRepository.save(product)
4. ProductRepository → PostgreSQL (UPDATE products ...)
5. PostgreSQL → ProductRepository → ProductService
6. ProductService → Spring Cache (processes @CacheEvict)
7. Spring Cache → Redis (DEL "products::1")
8. ProductService → ProductController → Client (200 OK)

Total: 100-200ms
```

## Next Steps

- See [Context Diagram](C4-Context-Diagram.md) for system overview
- See [Container Diagram](C4-Container-Diagram.md) for high-level architecture
- See [Architecture Overview](ARCHITECTURE_OVERVIEW.md) for detailed design
