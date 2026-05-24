# RateLimiterService

Redis-backed rate limiter service built with Java 17, Spring Boot 3, Redis 7, Lettuce, Lua scripts, Docker Compose, Prometheus, JUnit 5, and Mockito.

## Features in this version

- Token bucket rate limiting with max burst capacity and refill rate per second.
- Atomic Redis Lua scripts so check-and-increment happens in one Redis round trip.
- Sliding window log rate limiting with Redis sorted sets.
- API-key limits with `X-API-Key`, defaulting to `100 req/min`.
- IP limits when no API key is present, with `X-Forwarded-For` support.
- Unit tests for algorithm boundaries, burst rejection, Lua arguments, API-key extraction, IP extraction, and 429 responses.
- Docker Compose for the service, Redis 7, and Prometheus scraping `/actuator/prometheus`.
- `@RateLimit` annotation backed by Spring AOP.
- `@EnableRateLimiting` plus Spring Boot auto-configuration metadata for starter-style use.
- Dynamic admin API for updating limits in Redis without restarting the app.
- Redis Cluster key hash-tag support for Lua script slot safety.
- Custom Prometheus metrics for allowed/rejected requests and rate-limit latency.
- Structured JSON logs for rejected requests.
- JMeter benchmark plan in `benchmarks/`.

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
  filter-enabled: true
  redis-cluster-mode: false
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

When `rate-limiter.redis-cluster-mode=true`, Redis keys are hash-tagged around the identifier:

```text
rl:token-bucket:user-id:{user-123}:POST:_api_checkout
```

That matters in Redis Cluster because Lua scripts can only operate on keys that live in the same hash slot. The `{user-123}` tag forces all related keys for that user onto the same slot.

## HTTP behavior

The filter protects application endpoints except `/actuator`, `/admin`, `/error`, and `/favicon.ico`.

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

## Method annotations

Use `@RateLimit` when a controller method needs a specific policy:

```java
@PostMapping("/checkout")
@RateLimit(limit = 20, windowSeconds = 60, type = IdentifierType.USER_ID, includeEndpoint = true)
Map<String, String> checkout() {
    return Map.of("status", "accepted");
}
```

For `USER_ID`, send the user ID in `X-User-Id`. With `includeEndpoint = true`, the Redis key becomes a composite identity such as:

```text
user-123:POST:/api/checkout
```

If you use the project as a library in another Spring Boot app, add the artifact as a Maven dependency and enable it:

```java
@SpringBootApplication
@EnableRateLimiting
public class CheckoutApplication {
}
```

This project also includes Spring Boot auto-configuration metadata in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

## Admin API

Update limits at runtime:

```bash
curl -X POST http://localhost:8080/admin/limits \
  -H "Content-Type: application/json" \
  -d '{
    "identifierType": "USER_ID",
    "identifier": "user-123",
    "method": "POST",
    "endpoint": "/api/checkout",
    "algorithm": "TOKEN_BUCKET",
    "limit": 10,
    "windowSeconds": 60
  }'
```

The rule is stored in Redis under `rl:config:limits`, so every running service instance sees the same updated limit.

## Observability

Spring Boot Actuator exposes Prometheus metrics at:

```text
GET /actuator/prometheus
```

The Docker Compose Prometheus container scrapes that endpoint every 10 seconds. Open Prometheus at:

```text
http://localhost:9090
```

Useful queries:

```promql
requests_allowed_total
requests_rejected_total
rate_limit_check_duration_ms_count
http_server_requests_seconds_count
```

Every rejected request also writes a structured JSON log with user ID when available, identifier type, identifier, endpoint, limit, window, retry delay, and timestamp.

## Benchmarks

The JMeter plan lives at:

```text
benchmarks/rate-limiter-benchmark.jmx
```

Example non-GUI run:

```bash
jmeter -n \
  -t benchmarks/rate-limiter-benchmark.jmx \
  -JTHREADS=200 \
  -JDURATION_SECONDS=60 \
  -l benchmarks/results.jtl
```

Use separate runs for the README benchmark table. JMeter is not bundled with this repo, so these result cells should be filled after running on your machine or a benchmark VM:

| Target throughput | Threads | Duration | Result |
| --- | ---: | ---: | --- |
| 10K req/sec | TBD | 60s | TBD |
| 50K req/sec | TBD | 60s | TBD |
| 100K req/sec | TBD | 60s | TBD |

## Tests

```bash
mvn test
```

The current tests mock Redis through `StringRedisTemplate`, which keeps the unit suite fast and focused. The next step is an integration/concurrency test using Redis 7 to prove that exactly `N` requests succeed under simultaneous load.

## Remaining roadmap

- Fixed window counter baseline.
- Sliding window counter with interpolation.
- Concurrent correctness test with 100 threads.
- Benchmark result table for 10K, 50K, and 100K req/sec after running JMeter on a stable machine.
