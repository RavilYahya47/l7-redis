# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records for the Redis Caching Patterns project.

## What are ADRs?

Architecture Decision Records (ADRs) document important architectural decisions made during the development of this project. Each ADR captures:

- The context and forces that led to a decision
- The decision itself
- The consequences of the decision
- Alternatives considered and why they were rejected

## ADR Index

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [001](ADR-001-Use-Redis-for-Caching.md) | Use Redis for Caching Layer | Accepted | 2024 |
| [002](ADR-002-Choose-Lettuce-Over-Jedis.md) | Choose Lettuce Over Jedis | Accepted | 2024 |
| [003](ADR-003-Implement-Cache-Aside-Pattern.md) | Implement Cache-Aside Pattern | Accepted | 2024 |

## ADR Lifecycle

- **Proposed** - Under discussion
- **Accepted** - Decision made and implemented
- **Deprecated** - No longer recommended
- **Superseded** - Replaced by another ADR

## Creating a New ADR

Use the [ADR-TEMPLATE.md](ADR-TEMPLATE.md) as a starting point.

## Further Reading

- [Architecture Decision Records](https://adr.github.io/)
- [Documenting Architecture Decisions](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
