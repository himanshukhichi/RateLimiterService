package com.himanshu.ratelimiter.algorithm;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class TokenBucketRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RateLimiterKeyFactory keyFactory;
    private final DefaultRedisScript<List> script;

    public TokenBucketRateLimiter(StringRedisTemplate redisTemplate, RateLimiterKeyFactory keyFactory) {
        this.redisTemplate = redisTemplate;
        this.keyFactory = keyFactory;
        this.script = new DefaultRedisScript<>();
        this.script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/token_bucket.lua")));
        this.script.setResultType(List.class);
    }

    @Override
    public RateLimitAlgorithm algorithm() {
        return RateLimitAlgorithm.TOKEN_BUCKET;
    }

    @Override
    public RateLimitDecision check(RateLimitRequest request) {
        String key = keyFactory.keyFor(request);
        long nowMillis = request.now().toEpochMilli();
        List<?> result = redisTemplate.execute(
                script,
                List.of(key),
                String.valueOf(request.tokenBucketCapacity()),
                String.valueOf(request.tokenBucketRefillRatePerSecond()),
                String.valueOf(nowMillis),
                "1"
        );

        if (result == null || result.size() < 4) {
            throw new IllegalStateException("Redis token bucket script returned no decision");
        }

        boolean allowed = asLong(result.get(0)) == 1L;
        long remaining = asLong(result.get(1));
        Instant resetAt = Instant.ofEpochMilli(asLong(result.get(2)));
        Duration retryAfter = Duration.ofMillis(Math.max(0, asLong(result.get(3))));
        return new RateLimitDecision(
                allowed,
                request.tokenBucketCapacity(),
                remaining,
                resetAt,
                retryAfter,
                request.window(),
                algorithm(),
                request.identifierType(),
                request.identifier(),
                request.method(),
                request.endpoint()
        );
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }
}
