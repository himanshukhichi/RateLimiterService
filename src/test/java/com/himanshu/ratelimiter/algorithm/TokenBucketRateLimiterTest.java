package com.himanshu.ratelimiter.algorithm;

import com.himanshu.ratelimiter.config.RateLimiterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBucketRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private TokenBucketRateLimiter limiter;
    private final Instant now = Instant.parse("2026-05-23T10:15:30Z");

    @BeforeEach
    void setUp() {
        limiter = new TokenBucketRateLimiter(redisTemplate, new RateLimiterKeyFactory(new RateLimiterProperties()));
    }

    @Test
    void allowsRequestWhenExactlyOneTokenRemains() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(1L, 0L, now.plusSeconds(60).toEpochMilli(), 0L));

        RateLimitDecision decision = limiter.check(request(100, 10.0));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.limit()).isEqualTo(100);
        assertThat(decision.remaining()).isZero();
        assertThat(decision.retryAfter()).isEqualTo(Duration.ZERO);
    }

    @Test
    void rejectsBurstWhenBucketIsEmpty() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(0L, 0L, now.plusSeconds(10).toEpochMilli(), 500L));

        RateLimitDecision decision = limiter.check(request(10, 2.0));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.remaining()).isZero();
        assertThat(decision.retryAfter()).isEqualTo(Duration.ofMillis(500));
    }

    @Test
    void sendsCapacityRefillAndTimestampToLuaInOneRedisCall() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(1L, 99L, now.plusSeconds(1).toEpochMilli(), 0L));

        limiter.check(request(100, 1.6667));

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), argsCaptor.capture());

        assertThat(keysCaptor.getValue()).containsExactly("rl:token-bucket:api-key:test-key");
        assertThat(argsCaptor.getValue()).containsExactly(
                "100",
                "1.6667",
                String.valueOf(now.toEpochMilli()),
                "1"
        );
    }

    private RateLimitRequest request(int capacity, double refillRatePerSecond) {
        return new RateLimitRequest(
                IdentifierType.API_KEY,
                "test-key",
                RateLimitAlgorithm.TOKEN_BUCKET,
                capacity,
                Duration.ofMinutes(1),
                capacity,
                refillRatePerSecond,
                now,
                null,
                null
        );
    }
}
