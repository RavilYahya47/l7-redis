# C4 Context Diagram: Redis Caching Patterns

## Overview

The Context diagram shows the Redis Caching Patterns system and how it fits into the wider world, showing the people and systems that interact with it.

## Diagram

```
                 ┌──────────────────────────────────────────┐
                 │                                          │
                 │     Redis Caching Patterns System       │
                 │                                          │
                 │  Product Catalog Service demonstrating   │
                 │  Redis caching patterns, rate limiting,  │
                 │  distributed locks, and session mgmt     │
                 │                                          │
                 └──────────────────────────────────────────┘
                         ▲                   │
                         │                   │
                         │                   │
              Reads/Writes Products          │ Stores
              Manages Sessions               │ Product Data
              Views Leaderboards             │ User Data
                         │                   │
                         │                   ▼
           ┌─────────────┴──────────┐    ┌──────────────────┐
           │                        │    │                  │
           │   Web/Mobile Users     │    │   PostgreSQL     │
           │                        │    │   Database       │
           │  - Browse products     │    │                  │
           │  - Search catalog      │    │  Primary data    │
           │  - View leaderboards   │    │  store           │
           │  - Manage sessions     │    │                  │
           │                        │    └──────────────────┘
           └────────────────────────┘
                         │
                         │
                         │ API Requests
                         │ (Rate Limited)
                         │
                         ▼
           ┌──────────────────────────┐
           │                          │
           │  External Monitoring     │
           │  (Prometheus/Grafana)    │
           │                          │
           │  - Scrapes metrics       │
           │  - Visualizes dashboards │
           │  - Alerts on issues      │
           │                          │
           └──────────────────────────┘
```

## Elements

### Users (People)

#### Web/Mobile Users
**Type:** Person
**Description:** End users browsing the product catalog, viewing leaderboards, and managing sessions.

**Responsibilities:**
- Browse and search products
- View product details
- Check real-time leaderboards
- Manage shopping sessions

**Interactions:**
- Makes HTTP REST API calls to the system
- Receives responses (cached when possible)
- Subject to rate limiting for API protection

---

### Systems

#### Redis Caching Patterns System
**Type:** Software System
**Description:** Educational microservice demonstrating production-grade Redis caching patterns.

**Key Features:**
- Product catalog with intelligent caching
- API rate limiting (3 algorithms)
- Real-time leaderboards
- Distributed session management
- Distributed locking for coordination

**Technology Stack:**
- Java 17
- Spring Boot 3.2
- Redis 7
- PostgreSQL 16

---

#### PostgreSQL Database
**Type:** External System (Database)
**Description:** Relational database serving as the primary data store.

**Stores:**
- Product catalog (products, categories)
- User accounts
- Transactional data

**Relationship:**
- System reads from database on cache miss
- System writes to database on updates
- System uses database as source of truth

---

#### External Monitoring (Prometheus/Grafana)
**Type:** External System (Monitoring)
**Description:** Monitoring and observability platform.

**Responsibilities:**
- Scrape metrics from application
- Store time-series data
- Visualize dashboards
- Alert on anomalies

**Metrics Tracked:**
- Cache hit ratio
- Request latency (p50, p95, p99)
- Error rates
- Redis memory usage

---

## Key Interactions

### 1. User → System (Primary Flow)
```
User requests product information
→ System checks Redis cache
→ Cache hit: Return immediately (5-15ms)
→ Cache miss: Query PostgreSQL (50-100ms), cache result, return
```

### 2. System → PostgreSQL (Data Persistence)
```
User updates product
→ System validates request
→ System updates PostgreSQL
→ System invalidates Redis cache
→ Return success
```

### 3. System → Prometheus (Observability)
```
Prometheus scrapes /actuator/prometheus
← System exposes metrics
→ Grafana visualizes metrics
→ Alerts trigger on thresholds
```

## Business Context

### Goals
- **Education**: Demonstrate Redis patterns for course students
- **Performance**: 10x faster responses via caching
- **Scalability**: Handle 10x traffic increase
- **Resilience**: Gracefully degrade when cache unavailable

### Constraints
- Must use open-source technologies
- Must run on developer laptops (Docker Compose)
- Must demonstrate best practices
- Must be well-documented for learning

### Risks
| Risk | Impact | Mitigation |
|------|--------|------------|
| Redis unavailable | Medium | Fail-open to database |
| Database overload | High | Caching reduces load by 80% |
| Stale cached data | Low | 5-min TTL + event-based invalidation |
| API abuse | Medium | Rate limiting (3 algorithms) |

## External Dependencies

### Required Services
- **PostgreSQL**: Primary data store
- **Redis**: Caching and coordination layer

### Optional Services
- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization
- **Redis Insight**: Redis data exploration

## Deployment Context

### Development Environment
```
Developer Laptop
├─ Docker Compose
│  ├─ PostgreSQL container
│  ├─ Redis container
│  ├─ Redis Sentinel container
│  ├─ Redis Insight container
│  └─ Prometheus container
└─ Spring Boot Application (./gradlew bootRun)
```

### Production Environment (Conceptual)
```
Cloud Provider (AWS/Azure/GCP)
├─ Load Balancer
├─ App Instances (3+)
│  └─ Spring Boot containers
├─ Redis Cluster
│  ├─ Master nodes (3)
│  ├─ Replica nodes (3)
│  └─ Sentinel nodes (3)
└─ PostgreSQL
   ├─ Primary
   └─ Read Replicas (2+)
```

## Quality Attributes

### Performance
- **Cached reads**: < 20ms (p95)
- **Uncached reads**: < 100ms (p95)
- **Writes**: < 200ms (p95)
- **Throughput**: 1000 req/s per instance

### Scalability
- **Horizontal**: Add application instances (stateless)
- **Vertical**: Increase Redis/PostgreSQL resources
- **Target**: 10,000 req/s with 10 instances

### Availability
- **Target**: 99.9% uptime
- **Degraded mode**: Serve from database if Redis down
- **Recovery time**: < 1 minute (auto-restart)

### Security
- **Rate limiting**: Prevent API abuse
- **Input validation**: Prevent injection attacks
- **HTTPS**: Encrypt data in transit (production)
- **Database credentials**: Environment variables

## Next Steps

- See [C4 Container Diagram](C4-Container-Diagram.md) for internal architecture
- See [C4 Component Diagram](C4-Component-Diagram.md) for code structure
- See [Architecture Overview](ARCHITECTURE_OVERVIEW.md) for detailed design
