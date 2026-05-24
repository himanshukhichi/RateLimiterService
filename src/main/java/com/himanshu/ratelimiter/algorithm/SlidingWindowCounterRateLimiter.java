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
public class SlidingWindowCounterRateLimiter implements RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RateLimiterKeyFactory keyFactory;
    private final DefaultRedisScript<List> script;

    public SlidingWindowCounterRateLimiter(StringRedisTemplate redisTemplate, RateLimiterKeyFactory keyFactory) {
        this.redisTemplate = redisTemplate;
        this.keyFactory = keyFactory;
        this.script = new DefaultRedisScript<>();
        this.script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/sliding_window_counter.lua")));
        this.script.setResultType(List.class);
    }

    @Override
    public RateLimitAlgorithm algorithm() {
        return RateLimitAlgorithm.SLIDING_WINDOW_COUNTER;
    }

    @Override
    public RateLimitDecision check(RateLimitRequest request) {
        long nowMillis = request.now().toEpochMilli();
        long windowMillis = request.window().toMillis();
        List<?> result = redisTemplate.execute(
                script,
                List.of(keyFactory.keyFor(request)),
                String.valueOf(nowMillis),
                String.valueOf(windowMillis),
                String.valueOf(request.limit())
        );

        if (result == null || result.size() < 4) {
            throw new IllegalStateException("Redis sliding window counter script returned no decision");
        }

        boolean allowed = asLong(result.get(0)) == 1L;
        long remaining = asLong(result.get(1));
        Instant resetAt = Instant.ofEpochMilli(asLong(result.get(2)));
        Duration retryAfter = Duration.ofMillis(Math.max(0, asLong(result.get(3))));
        return new RateLimitDecision(
                allowed,
                request.limit(),
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
