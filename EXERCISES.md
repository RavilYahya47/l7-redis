# Hands-On Exercises: Redis Caching Patterns

## Overview

These exercises will help you understand Redis caching patterns through practical experimentation. Each exercise builds on concepts from KEY_CONCEPTS.md and uses the running application.

**Prerequisites:**
- Application running (`./gradlew bootRun`)
- Docker services up (`docker-compose up -d`)
- Redis Insight open (http://localhost:8001)
- Basic curl or Postman knowledge

---

## Exercise 1: Cache Performance Analysis

**Objective:** Measure and understand cache performance impact.

### Part A: Baseline Measurement

1. Clear all caches:
```bash
curl -X POST http://localhost:8080/api/products/cache/clear
```

2. Measure database query time:
```bash
time curl http://localhost:8080/api/products/1
```

3. Check application logs for "Fetching product from database" message.

4. Repeat 5 times and record average response time.

### Part B: Cache Hit Measurement

1. Make the same request again (cache should be populated):
```bash
time curl http://localhost:8080/api/products/1
```

2. Repeat 5 times and record average response time.

3. In Redis Insight, find the cached entry:
   - Browse keys
   - Look for `products::1`
   - Examine the JSON structure

### Part C: Analysis

**Questions to answer:**

1. What was the performance improvement (cache hit vs cache miss)?
   - Expected: 5-10x faster

2. How long is the cached value valid?
   - Check TTL in Redis Insight
   - Expected: 5 minutes (300 seconds)

3. What happens when you update the product?
```bash
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"price": 199.99}'
```
   - Is the cache invalidated?
   - Check Redis Insight to verify

### Expected Results

| Metric | Without Cache | With Cache | Improvement |
|--------|---------------|------------|-------------|
| Response Time | 50-100ms | 5-15ms | 5-10x |
| DB Queries | 1 | 0 | 100% reduction |
| Cache Hit Ratio | 0% | 100% | N/A |

---

## Exercise 2: Rate Limiting Experiment

**Objective:** Test different rate limiting algorithms and understand their behavior.

### Part A: Fixed Window Rate Limiting

1. Configure fixed window rate limit (10 requests per 60 seconds):

2. Send 15 requests rapidly:
```bash
for i in {1..15}; do
  echo "Request $i:"
  curl -s http://localhost:8080/api/rate-limit/fixed-window/testuser?maxRequests=10&windowSeconds=60 | jq
  sleep 0.5
done
```

3. Observe when you start getting 429 (Too Many Requests) responses.

4. In Redis Insight, examine the rate limit key:
   - Key: `rate_limit:fixed:testuser`
   - Check the current count
   - Check the TTL

### Part B: Boundary Burst Test

1. Wait for the 60-second window to expire.

2. Send 10 requests at second 59 of the window:
```bash
sleep 59  # Wait until end of window
for i in {1..10}; do
  curl -s http://localhost:8080/api/rate-limit/fixed-window/testuser?maxRequests=10&windowSeconds=60
done
```

3. Immediately send 10 more requests at second 0 of next window:
```bash
for i in {1..10}; do
  curl -s http://localhost:8080/api/rate-limit/fixed-window/testuser?maxRequests=10&windowSeconds=60
done
```

**Question:** Did you just send 20 requests in 2 seconds despite a limit of 10/minute?

### Part C: Sliding Window Comparison

1. Reset rate limit:
```bash
curl -X POST http://localhost:8080/api/rate-limit/reset/testuser
```

2. Test sliding window with same limits:
```bash
for i in {1..15}; do
  echo "Request $i:"
  curl -s http://localhost:8080/api/rate-limit/sliding-window/testuser?maxRequests=10&windowSeconds=60 | jq
  sleep 0.5
done
```

3. Try the boundary burst attack again.

**Question:** Does sliding window prevent the boundary burst?

### Part D: Token Bucket

1. Test token bucket (capacity: 10, refill: 2 tokens/second):
```bash
# Burst 10 requests immediately (should all succeed)
for i in {1..10}; do
  curl -s http://localhost:8080/api/rate-limit/token-bucket/testuser?capacity=10&refillRate=2.0&tokensRequired=1
done

# 11th request should fail (no tokens)
curl -s http://localhost:8080/api/rate-limit/token-bucket/testuser?capacity=10&refillRate=2.0&tokensRequired=1

# Wait 5 seconds (10 tokens refilled)
sleep 5

# These should succeed
for i in {1..10}; do
  curl -s http://localhost:8080/api/rate-limit/token-bucket/testuser?capacity=10&refillRate=2.0&tokensRequired=1
done
```

### Analysis Questions

1. Which algorithm is simplest to implement?
2. Which algorithm prevents boundary bursts?
3. Which algorithm allows controlled bursts?
4. Which would you use for:
   - Login attempts?
   - API requests?
   - Payment processing?

---

## Exercise 3: Distributed Lock Coordination

**Objective:** Understand distributed locks and race conditions.

### Part A: Simulate Race Condition

1. Start 2 terminal windows.

2. In both terminals, run this command simultaneously:
```bash
# Terminal 1
curl -X POST "http://localhost:8080/api/locks/inventory-update/execute?waitTime=0&leaseTime=10"

# Terminal 2 (run immediately after Terminal 1)
curl -X POST "http://localhost:8080/api/locks/inventory-update/execute?waitTime=0&leaseTime=10"
```

**Expected:**
- Terminal 1: Success (lock acquired)
- Terminal 2: Failure or timeout (lock held by Terminal 1)

3. Check application logs to see which terminal got the lock.

### Part B: Lock Wait Behavior

1. In Terminal 1, acquire a lock:
```bash
curl -X POST "http://localhost:8080/api/locks/test-lock/try?leaseTime=30"
```

2. In Terminal 2, try to execute with the same lock (with wait):
```bash
time curl -X POST "http://localhost:8080/api/locks/test-lock/execute?waitTime=5&leaseTime=10"
```

**Question:** How long did Terminal 2 wait before failing?

3. Release the lock from Terminal 1:
```bash
curl -X POST "http://localhost:8080/api/locks/test-lock/unlock"
```

4. Now Terminal 2 can acquire the lock.

### Part C: Lock Expiration

1. Acquire a lock with short lease time:
```bash
curl -X POST "http://localhost:8080/api/locks/expiry-test/try?leaseTime=5"
```

2. Check lock status:
```bash
curl http://localhost:8080/api/locks/expiry-test/status
```

3. Wait 6 seconds and check again:
```bash
sleep 6
curl http://localhost:8080/api/locks/expiry-test/status
```

**Question:** Did the lock auto-release after lease time?

### Real-World Scenario

**Inventory Update Without Lock (Race Condition)**

Imagine 2 users buying the last item simultaneously:

1. User A reads stock: 1 item
2. User B reads stock: 1 item
3. User A decrements and writes: 0 items
4. User B decrements and writes: 0 items
Result: Stock is 0, but only User A got confirmation!

**With Distributed Lock:**

1. User A acquires lock
2. User A reads stock: 1 item
3. User A decrements and writes: 0 items
4. User A releases lock
5. User B acquires lock
6. User B reads stock: 0 items
7. User B gets "out of stock" error
Result: Correct! Only one user gets the item.

---

## Exercise 4: Leaderboard Operations

**Objective:** Build and query real-time leaderboards using sorted sets.

### Part A: Populate Leaderboard

1. Add players with scores:
```bash
curl -X POST "http://localhost:8080/api/leaderboard/game/score?playerId=alice&score=1500"
curl -X POST "http://localhost:8080/api/leaderboard/game/score?playerId=bob&score=2000"
curl -X POST "http://localhost:8080/api/leaderboard/game/score?playerId=charlie&score=1200"
curl -X POST "http://localhost:8080/api/leaderboard/game/score?playerId=david&score=1800"
curl -X POST "http://localhost:8080/api/leaderboard/game/score?playerId=eve&score=2500"
curl -X POST "http://localhost:8080/api/leaderboard/game/score?playerId=frank&score=1600"
curl -X POST "http://localhost:8080/api/leaderboard/game/score?playerId=grace&score=2200"
curl -X POST "http://localhost:8080/api/leaderboard/game/score?playerId=henry&score=1400"
```

2. In Redis Insight, examine the sorted set:
   - Key: `leaderboard:game`
   - Type: ZSET
   - View members and scores

### Part B: Query Leaderboard

1. Get top 3 players:
```bash
curl http://localhost:8080/api/leaderboard/game/top?count=3 | jq
```

2. Get Bob's rank:
```bash
curl http://localhost:8080/api/leaderboard/game/rank/bob | jq
```

3. Get players around Bob (neighboring ranks):
```bash
curl http://localhost:8080/api/leaderboard/game/around/bob?range=2 | jq
```

### Part C: Real-Time Updates

1. Increment scores (simulating game progress):
```bash
# Alice scores 100 points
curl -X POST "http://localhost:8080/api/leaderboard/game/increment?playerId=alice&delta=100"

# Bob scores 50 points
curl -X POST "http://localhost:8080/api/leaderboard/game/increment?playerId=bob&delta=50"

# Charlie scores 1000 points (big win!)
curl -X POST "http://localhost:8080/api/leaderboard/game/increment?playerId=charlie&delta=1000"
```

2. Check updated rankings:
```bash
curl http://localhost:8080/api/leaderboard/game/top?count=3 | jq
```

**Question:** Did Charlie move up in rankings?

### Part D: Performance Test

1. Add 1000 players with random scores:
```bash
for i in {1..1000}; do
  score=$((RANDOM % 10000))
  curl -X POST "http://localhost:8080/api/leaderboard/game/score?playerId=player$i&score=$score" &
done
wait
```

2. Time the top 100 query:
```bash
time curl http://localhost:8080/api/leaderboard/game/top?count=100
```

**Question:** How fast is the query even with 1000+ players?
**Expected:** < 10ms (O(log N) complexity)

---

## Exercise 5: Session Management

**Objective:** Implement stateless session management.

### Part A: Create and Manage Sessions

1. Create a session for user 1:
```bash
SESSION_ID=$(curl -X POST "http://localhost:8080/api/sessions?userId=1" \
  -H "Content-Type: application/json" \
  -d '{"theme": "dark", "language": "en"}' | tr -d '"')

echo "Session ID: $SESSION_ID"
```

2. Retrieve session:
```bash
curl http://localhost:8080/api/sessions/$SESSION_ID | jq
```

3. Add items to shopping cart:
```bash
curl -X PUT http://localhost:8080/api/sessions/$SESSION_ID/attributes/cart \
  -H "Content-Type: application/json" \
  -d '["PROD-001", "PROD-002", "PROD-003"]'
```

4. Verify cart was saved:
```bash
curl http://localhost:8080/api/sessions/$SESSION_ID/attributes/cart | jq
```

### Part B: Multi-Device Sessions

1. Create 3 sessions for the same user (simulating 3 devices):
```bash
SESSION1=$(curl -X POST "http://localhost:8080/api/sessions?userId=100" -d '{"device":"phone"}' -H "Content-Type: application/json" | tr -d '"')
SESSION2=$(curl -X POST "http://localhost:8080/api/sessions?userId=100" -d '{"device":"tablet"}' -H "Content-Type: application/json" | tr -d '"')
SESSION3=$(curl -X POST "http://localhost:8080/api/sessions?userId=100" -d '{"device":"desktop"}' -H "Content-Type: application/json" | tr -d '"')
```

2. Get all active sessions:
```bash
curl http://localhost:8080/api/sessions/user/100 | jq
```

3. Invalidate all sessions (logout from all devices):
```bash
curl -X DELETE http://localhost:8080/api/sessions/user/100
```

4. Verify sessions are gone:
```bash
curl http://localhost:8080/api/sessions/user/100 | jq
```

### Part C: Session Expiration

1. Create a session and note the creation time:
```bash
SESSION_ID=$(curl -X POST "http://localhost:8080/api/sessions?userId=200" | tr -d '"')
curl http://localhost:8080/api/sessions/$SESSION_ID | jq '.createdAt'
```

2. In Redis Insight, check the TTL:
   - Find key: `session:$SESSION_ID`
   - Check TTL (should be 30 minutes = 1800 seconds)

3. Access the session (triggers TTL refresh):
```bash
curl http://localhost:8080/api/sessions/$SESSION_ID
```

4. Check TTL again in Redis Insight.

**Question:** Did accessing the session reset the TTL to 30 minutes?

---

## Exercise 6: Cache Invalidation Strategies

**Objective:** Implement and test different cache invalidation approaches.

### Part A: Time-Based Expiration (TTL)

1. Get a product (populates cache with 5-minute TTL):
```bash
curl http://localhost:8080/api/products/1
```

2. Check cache in Redis Insight:
   - Key: `products::1`
   - Check TTL

3. Wait 5+ minutes (or use Redis CLI to delete the key).

4. Request again and check logs for database fetch.

### Part B: Event-Based Invalidation

1. Get a product (cache warm):
```bash
curl http://localhost:8080/api/products/1
```

2. Update the product:
```bash
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Updated Product Name"}'
```

3. Check Redis Insight - is the cache entry still there?

4. Get the product again:
```bash
curl http://localhost:8080/api/products/1
```

**Question:** Did the updated name appear immediately or after TTL expiry?

### Part C: Bulk Invalidation

1. Warm up multiple product caches:
```bash
for i in {1..5}; do
  curl http://localhost:8080/api/products/$i
done
```

2. Verify caches in Redis Insight (should see 5 `products::*` keys).

3. Clear all product caches:
```bash
curl -X POST http://localhost:8080/api/products/cache/clear
```

4. Check Redis Insight - all `products::*` keys should be gone.

---

## Exercise 7: Build a Custom Caching Feature

**Objective:** Apply learned concepts to implement a new feature.

### Challenge: Implement "Recently Viewed Products"

**Requirements:**
1. Track last 10 products viewed by each user
2. Products stored in order (most recent first)
3. Use Redis List data structure
4. API endpoint: `GET /api/users/{userId}/recent`

### Steps

1. Create a new service method:
```java
public void trackViewedProduct(Long userId, Long productId) {
    String key = "recent_views:" + userId;

    // Add to front of list
    redisTemplate.opsForList().leftPush(key, productId);

    // Keep only 10 items
    redisTemplate.opsForList().trim(key, 0, 9);

    // Set 30-day expiration
    redisTemplate.expire(key, 30, TimeUnit.DAYS);
}
```

2. Call this method in ProductService.getProduct():
```java
@Cacheable(value = "products", key = "#id")
public ProductResponse getProduct(Long id, Long userId) {
    // Existing code...

    if (userId != null) {
        trackViewedProduct(userId, id);
    }

    return product;
}
```

3. Create endpoint to retrieve recent views:
```java
@GetMapping("/users/{userId}/recent")
public List<ProductResponse> getRecentlyViewed(@PathVariable Long userId) {
    String key = "recent_views:" + userId;
    List<Object> productIds = redisTemplate.opsForList().range(key, 0, 9);

    return productIds.stream()
        .map(id -> productService.getProduct((Long)id))
        .collect(Collectors.toList());
}
```

4. Test it:
```bash
# View some products
curl http://localhost:8080/api/products/1
curl http://localhost:8080/api/products/2
curl http://localhost:8080/api/products/3

# Get recently viewed
curl http://localhost:8080/api/users/1/recent
```

**Bonus:** Add pagination to handle users who view >10 products.

---

## Reflection Questions

After completing these exercises, answer:

1. **When should you use caching?**
   - What workload patterns benefit most?
   - What are the trade-offs?

2. **Which rate limiting algorithm fits your use case?**
   - API rate limiting: _____
   - Login attempt throttling: _____
   - Why?

3. **When do you need distributed locks?**
   - Give 3 real-world examples
   - What happens without locks in those scenarios?

4. **How do you choose cache TTL?**
   - Frequently changing data: _____
   - Rarely changing data: _____
   - Real-time data: _____

5. **What's the biggest challenge with caching?**
   - Cache invalidation
   - Memory management
   - Consistency
   - Other: _____

---

## Next Steps

1. Review your answers with [KEY_CONCEPTS.md](KEY_CONCEPTS.md)
2. Explore [Architecture Decision Records](docs/adr/) for design rationale
3. Read [LINKS.md](LINKS.md) for deeper dives into specific topics
4. Experiment with your own caching scenarios
5. Try implementing Pub/Sub messaging (bonus challenge!)

**Congratulations!** You've completed hands-on practice with Redis caching patterns. These patterns are used in production systems handling millions of requests per day.
