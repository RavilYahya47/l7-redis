# Learning Resources: Redis Caching Patterns

## Official Documentation

### Redis
- [Redis Official Documentation](https://redis.io/docs/) - Comprehensive Redis documentation
- [Redis Commands Reference](https://redis.io/commands/) - Complete command reference
- [Redis Data Types](https://redis.io/docs/data-types/) - Deep dive into data structures
- [Redis Persistence](https://redis.io/docs/management/persistence/) - RDB and AOF explained
- [Redis Replication](https://redis.io/docs/management/replication/) - Master-replica setup
- [Redis Sentinel](https://redis.io/docs/management/sentinel/) - High availability with Sentinel
- [Redis Cluster](https://redis.io/docs/management/scaling/) - Horizontal scaling

### Spring Data Redis
- [Spring Data Redis Documentation](https://spring.io/projects/spring-data-redis)
- [Spring Data Redis Reference](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Spring Boot Redis Starter](https://docs.spring.io/spring-boot/docs/current/reference/html/data.html#data.nosql.redis)

### Redisson
- [Redisson GitHub](https://github.com/redisson/redisson)
- [Redisson Documentation](https://github.com/redisson/redisson/wiki)
- [Redisson Distributed Locks](https://github.com/redisson/redisson/wiki/8.-Distributed-locks-and-synchronizers)
- [Redisson Objects](https://github.com/redisson/redisson/wiki/6.-Distributed-objects)

---

## Caching Strategies

### Articles and Guides
- [AWS: Caching Strategies](https://aws.amazon.com/caching/best-practices/) - AWS caching best practices
- [Caching Patterns](https://docs.aws.amazon.com/whitepapers/latest/database-caching-strategies-using-redis/caching-patterns.html) - Cache-aside, write-through, write-behind
- [Martin Fowler: Cache](https://martinfowler.com/bliki/TwoHardThings.html) - Two hard things in CS
- [High Scalability: Cache](http://highscalability.com/blog/category/cache) - Real-world caching stories
- [Cache Warming Strategies](https://www.sobyte.net/post/2022-03/cache-warming/) - Preventing cache stampede

### Cache Invalidation
- [Phil Karlton Quote Origin](https://www.karlton.org/2017/12/naming-things-hard/) - Cache invalidation is hard
- [Caching at Reddit](https://redditblog.com/2017/01/17/caching-at-reddit/) - Reddit's caching architecture
- [Facebook's Memcache](https://research.facebook.com/publications/scaling-memcache-at-facebook/) - Scaling cache
- [Cache Invalidation Patterns](https://simonwillison.net/2018/Jan/12/cache-invalidation/) - Practical approaches

---

## Rate Limiting

### Algorithms and Implementations
- [Token Bucket Algorithm](https://en.wikipedia.org/wiki/Token_bucket) - Wikipedia explanation
- [Leaky Bucket vs Token Bucket](https://www.baeldung.com/cs/leaky-bucket-vs-token-bucket) - Algorithm comparison
- [Rate Limiting Strategies](https://cloud.google.com/architecture/rate-limiting-strategies-techniques) - Google Cloud guide
- [Stripe Rate Limiting](https://stripe.com/blog/rate-limiters) - Stripe's approach
- [GitHub Rate Limiting](https://docs.github.com/en/rest/overview/resources-in-the-rest-api#rate-limiting) - Real-world example

### Best Practices
- [Redis-based Rate Limiter](https://engineering.classdojo.com/blog/2015/02/06/rolling-rate-limiter/) - Rolling window implementation
- [Distributed Rate Limiting](https://konghq.com/blog/how-to-design-a-scalable-rate-limiting-algorithm) - Kong's approach
- [NGINX Rate Limiting](https://www.nginx.com/blog/rate-limiting-nginx/) - Using NGINX

---

## Distributed Systems

### Distributed Locks
- [Redlock Algorithm](https://redis.io/docs/manual/patterns/distributed-locks/) - Redis official pattern
- [How to Do Distributed Locking](https://martin.kleppmann.com/2016/02/08/how-to-do-distributed-locking.html) - Martin Kleppmann's analysis
- [Is Redlock Safe?](http://antirez.com/news/101) - Antirez's response
- [Distributed Locks with Redis](https://redis.io/docs/reference/patterns/distributed-locks/) - Best practices

### Consistency and CAP Theorem
- [CAP Theorem](https://en.wikipedia.org/wiki/CAP_theorem) - Consistency, Availability, Partition tolerance
- [Eventual Consistency](https://www.allthingsdistributed.com/2008/12/eventually_consistent.html) - Werner Vogels (AWS CTO)
- [Strong vs Eventual Consistency](https://fauna.com/blog/strong-vs-eventual-consistency-in-distributed-databases) - Trade-offs

---

## Redis Data Structures

### In-Depth Guides
- [Redis Strings](https://redis.io/docs/data-types/strings/) - Strings and binary data
- [Redis Lists](https://redis.io/docs/data-types/lists/) - Linked lists
- [Redis Sets](https://redis.io/docs/data-types/sets/) - Unique elements
- [Redis Sorted Sets](https://redis.io/docs/data-types/sorted-sets/) - Leaderboards, rankings
- [Redis Hashes](https://redis.io/docs/data-types/hashes/) - Field-value pairs
- [Redis Streams](https://redis.io/docs/data-types/streams/) - Append-only logs
- [Redis HyperLogLog](https://redis.io/docs/data-types/hyperloglogs/) - Cardinality estimation
- [Redis Bloom Filters](https://redis.io/docs/stack/bloom/) - Probabilistic data structures

### Use Cases
- [Leaderboards with Redis](https://redis.io/topics/leaderboards) - Sorted sets in action
- [Real-time Analytics](https://redis.io/solutions/real-time-analytics) - Using Redis for analytics
- [Session Storage](https://redis.io/solutions/session-cache) - Managing sessions
- [Message Queues](https://redis.io/topics/streams-intro) - Redis Streams

---

## Performance and Optimization

### Benchmarking
- [redis-benchmark Tool](https://redis.io/docs/management/optimization/benchmarks/) - Built-in benchmarking
- [Optimizing Redis](https://redis.io/docs/management/optimization/) - Performance tuning
- [Redis Latency Doctor](https://redis.io/docs/reference/optimization/latency/) - Diagnosing latency issues

### Memory Optimization
- [Redis Memory Optimization](https://redis.io/docs/management/optimization/memory-optimization/) - Reducing memory usage
- [Eviction Policies](https://redis.io/docs/reference/eviction/) - maxmemory-policy options
- [Memory Analysis](https://redis.io/commands/memory-doctor/) - Memory usage analysis

### Connection Pooling
- [Lettuce Connection Pooling](https://github.com/lettuce-io/lettuce-core/wiki/Connection-Pooling) - Lettuce best practices
- [Jedis vs Lettuce](https://stackoverflow.com/questions/45867460/jedis-vs-lettuce) - Client comparison

---

## Real-World Case Studies

### Tech Company Blogs
- [Instagram Engineering](https://instagram-engineering.com/storing-hundreds-of-millions-of-simple-key-value-pairs-in-redis-1091ae80f74c) - Scaling Redis
- [Twitter: Redis at Twitter](https://blog.twitter.com/engineering/en_us/a/2014/redis-at-twitter) - Twitter's usage
- [Slack: Scaling Redis](https://slack.engineering/scaling-slacks-job-queue/) - Job queue with Redis
- [Airbnb: Avoiding Pitfalls](https://medium.com/airbnb-engineering/avoiding-common-redis-pitfalls-36d0c5e01324) - Lessons learned
- [Pinterest: Redis at Scale](https://medium.com/pinterest-engineering/how-pinterest-runs-kafka-at-scale-ff9c6f735be) - Large-scale Redis

### Conference Talks
- [Redis Conf Talks](https://www.youtube.com/c/Redisinc) - Official Redis conference
- [Strange Loop: Redis](https://www.youtube.com/results?search_query=strange+loop+redis) - Conference talks

---

## Books

### Redis
- **"Redis in Action"** by Josiah Carlson
  - Comprehensive Redis guide
  - Practical examples and patterns
  - [Manning Publications](https://www.manning.com/books/redis-in-action)

- **"Redis Essentials"** by Maxwell Dayvson Da Silva
  - Quick introduction
  - Common patterns
  - [Packt Publishing](https://www.packtpub.com/product/redis-essentials/9781784392451)

### Caching and Performance
- **"Web Scalability for Startup Engineers"** by Artur Ejsmont
  - Caching strategies
  - Real-world architectures
  - Scalability patterns

- **"Designing Data-Intensive Applications"** by Martin Kleppmann
  - Chapter on caching
  - Distributed systems
  - Must-read for senior engineers

---

## Tools and Utilities

### GUI Clients
- [Redis Insight](https://redis.com/redis-enterprise/redis-insight/) - Official Redis GUI (used in this project)
- [RedisDesktopManager](https://github.com/RedisInsight/RedisDesktopManager) - Open-source GUI
- [Medis](https://getmedis.com/) - macOS Redis GUI

### Monitoring
- [RedisLabs Prometheus Exporter](https://github.com/oliver006/redis_exporter) - Prometheus metrics
- [Grafana Redis Dashboard](https://grafana.com/grafana/dashboards/11835-redis-dashboard-for-prometheus-redis-exporter/) - Visualization
- [RedisLive](https://github.com/snakeliwei/RedisLive) - Real-time monitoring

### CLI Tools
- [redis-cli](https://redis.io/docs/ui/cli/) - Official command-line client
- [redis-benchmark](https://redis.io/docs/reference/optimization/benchmarks/) - Performance testing
- [redis-stat](https://github.com/junegunn/redis-stat) - Real-time stats

---

## Spring Boot Integration

### Tutorials
- [Baeldung: Spring Data Redis](https://www.baeldung.com/spring-data-redis-tutorial) - Complete tutorial
- [Spring Boot Redis Cache](https://www.baeldung.com/spring-boot-redis-cache) - Cache abstraction
- [Spring Session with Redis](https://spring.io/guides/gs/spring-session/) - Session management
- [Redis Pub/Sub with Spring](https://www.baeldung.com/spring-data-redis-pub-sub) - Messaging

### Examples
- [Spring Boot Redis Examples](https://github.com/spring-projects/spring-data-examples/tree/main/redis) - Official examples
- [Spring Cache Examples](https://github.com/spring-projects/spring-framework/tree/main/spring-context/src/test/java/org/springframework/cache) - Cache testing

---

## Advanced Topics

### Pub/Sub Messaging
- [Redis Pub/Sub](https://redis.io/docs/manual/pubsub/) - Official guide
- [Pub/Sub Patterns](https://redis.io/topics/pubsub) - Advanced patterns

### Transactions and Pipelines
- [Redis Transactions](https://redis.io/docs/manual/transactions/) - MULTI/EXEC
- [Pipelining](https://redis.io/docs/manual/pipelining/) - Batching commands

### Lua Scripting
- [Redis Lua Scripting](https://redis.io/docs/manual/programmability/eval-intro/) - EVAL and SCRIPT
- [Lua Scripts Examples](https://redis.io/commands/eval#examples) - Practical scripts

### Redis Modules
- [Redis Stack](https://redis.io/docs/stack/) - Extended Redis capabilities
- [RediSearch](https://redis.io/docs/stack/search/) - Full-text search
- [RedisJSON](https://redis.io/docs/stack/json/) - JSON data type
- [RedisGraph](https://redis.io/docs/stack/graph/) - Graph database
- [RedisTimeSeries](https://redis.io/docs/stack/timeseries/) - Time-series data

---

## Community and Forums

- [Redis Community](https://redis.io/community/) - Official community hub
- [Reddit r/redis](https://www.reddit.com/r/redis/) - Redis subreddit
- [Stack Overflow [redis]](https://stackoverflow.com/questions/tagged/redis) - Q&A
- [Redis Discord](https://discord.gg/redis) - Real-time chat
- [Redis Mailing List](https://groups.google.com/g/redis-db) - Discussions

---

## Courses and Video Tutorials

- [Redis University](https://university.redis.com/) - Free official courses
  - RU101: Introduction to Redis Data Structures
  - RU102J: Redis for Java Developers
  - RU202: Redis Streams
  - RU301: Running Redis at Scale

- [Udemy: Redis Bootcamp](https://www.udemy.com/course/redis-bootcamp-for-beginners/) - Beginner-friendly
- [Pluralsight: Redis](https://www.pluralsight.com/search?q=redis) - Multiple courses
- [YouTube: TechWorld with Nana](https://www.youtube.com/watch?v=OqCK95AS-YE) - Redis crash course

---

## Best Practices and Patterns

- [Redis Best Practices](https://redis.io/docs/manual/patterns/) - Official patterns
- [AWS Redis Best Practices](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/BestPractices.html) - Cloud deployment
- [12 Factor App: Backing Services](https://12factor.net/backing-services) - Architecture principles
- [Microservices Patterns: Caching](https://microservices.io/patterns/data/caching.html) - Microservice caching

---

## Interactive Learning

- [Try Redis](https://try.redis.io/) - Interactive Redis tutorial
- [Redis Playground](https://redis.io/try-free/) - Free Redis Cloud instance
- [Katacoda Redis Scenarios](https://www.katacoda.com/courses/redis) - Hands-on scenarios

---

## Related Technologies

### Alternatives to Redis
- [Memcached](https://memcached.org/) - Simple in-memory cache
- [Hazelcast](https://hazelcast.com/) - Distributed cache and compute
- [Apache Ignite](https://ignite.apache.org/) - In-memory computing platform

### Complementary Technologies
- [Apache Kafka](https://kafka.apache.org/) - Event streaming (pairs well with Redis)
- [PostgreSQL](https://www.postgresql.org/) - Primary database (use Redis as cache)
- [Elasticsearch](https://www.elastic.co/) - Search engine (Redis for real-time aggregations)

---

## Next Steps

After exploring these resources:

1. Complete the [EXERCISES.md](EXERCISES.md) to practice
2. Review [KEY_CONCEPTS.md](KEY_CONCEPTS.md) for theory
3. Read [Architecture Decision Records](docs/adr/) for design decisions
4. Join the Redis community and ask questions
5. Build your own Redis-powered project!

**Remember:** The best way to learn is by doing. Experiment, break things, and learn from mistakes!
