# RateLimiterService

Redis-backed rate limiter service built with Java 17, Spring Boot 3, Redis 7, Lettuce, Lua scripts, Docker Compose, Prometheus, JUnit 5, and Mockito.

## Core features in this version

- Token bucket rate limiting with max burst capacity and refill rate per second.
- Atomic Redis Lua scripts so check-and-increment happens in one Redis round trip.
- Sliding window log rate limiting with Redis sorted sets.
- API-key limits with `X-API-Key`, defaulting to `100 req/min`.
- IP limits when no API key is present, with `X-Forwarded-For` support.
- Unit tests for algorithm boundaries, burst rejection, Lua arguments, API-key extraction, IP extraction, and 429 responses.
- Docker Compose for the service, Redis 7, and Prometheus scraping `/actuator/prometheus`.

## Quick start

Build the app:

```bash
mvn clean package
```

Start Redis, the service, and Prometheus:

```bash
docker compose up --build
```

Call the demo endpoint:

```bash
curl -i -H "X-API-Key: demo-key" http://localhost:8080/api/ping
```

Switch to the sliding window log algorithm:

```bash
RATE_LIMITER_ALGORITHM=SLIDING_WINDOW_LOG docker compose up --build
```

Or set it in `src/main/resources/application.yml`:

```yaml
rate-limiter:
  algorithm: SLIDING_WINDOW_LOG
```

## Configuration

```yaml
rate-limiter:
  enabled: true
  algorithm: TOKEN_BUCKET
  api-key:
    limit: 100
    window: 60s
  ip:
    limit: 100
    window: 60s
```

`TOKEN_BUCKET` is best when you want a controlled average rate with short bursts. The configured `limit` becomes the maximum burst size, and the refill rate is derived from `limit / window`, so `100` over `60s` refills at about `1.67 tokens/sec`. `SLIDING_WINDOW_LOG` is stricter because it stores request timestamps and counts only the current rolling window.

## Redis data model

Token bucket stores a Redis hash per identifier:

```text
rl:token-bucket:api-key:<api-key>
  tokens
  updatedAt
```

Sliding window log stores request timestamps in a sorted set:

```text
rl:sliding-window-log:ip:<client-ip>
```

Both algorithms set key TTLs from Lua, so idle identifiers clean themselves up without a background job.

## HTTP behavior

The filter protects application endpoints except `/actuator`, `/error`, and `/favicon.ico`.

Request identity is resolved in this order:

1. `X-API-Key`
2. First IP in `X-Forwarded-For`
3. Servlet remote address

429 responses include:

```json
{"error":"rate_limit_exceeded","message":"Too many requests"}
```

The response also includes rate-limit headers:

- `X-RateLimit-Limit`
- `X-RateLimit-Remaining`
- `X-RateLimit-Reset`
- `Retry-After` on rejected requests

## Tests

```bash
mvn test
```

The current tests mock Redis through `StringRedisTemplate`, which keeps the unit suite fast and focused. The next step is an integration/concurrency test using Redis 7 to prove that exactly `N` requests succeed under simultaneous load.

## Roadmap

- Fixed window counter baseline.
- Sliding window counter with interpolation.
- Limit by user ID plus endpoint composite keys.
- Concurrent correctness test with 100 threads.
- Redis Cluster hash-tagged keys for Lua script slot safety.
- Spring Boot starter and `@RateLimit` annotation via AOP.
- Admin API for dynamic limits.
- Custom Prometheus counters and histograms.
- Structured JSON logging for 429 audit events.
- JMeter benchmark scripts and README benchmark table.
