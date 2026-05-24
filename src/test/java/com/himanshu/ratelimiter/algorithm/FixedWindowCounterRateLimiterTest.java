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
class FixedWindowCounterRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private FixedWindowCounterRateLimiter limiter;
    private final Instant now = Instant.parse("2026-05-23T10:15:30Z");

    @BeforeEach
    void setUp() {
        limiter = new FixedWindowCounterRateLimiter(redisTemplate, new RateLimiterKeyFactory(new RateLimiterProperties()));
    }

    @Test
    void allowsRequestWhenCounterIsExactlyAtLimitAfterIncrement() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(1L, 0L, now.plusSeconds(30).toEpochMilli(), 0L));

        RateLimitDecision decision = limiter.check(request(100));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.remaining()).isZero();
        assertThat(decision.retryAfter()).isEqualTo(Duration.ZERO);
    }

    @Test
    void rejectsRequestAfterWindowLimitIsExceeded() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(0L, 0L, now.plusSeconds(5).toEpochMilli(), 5_000L));

        RateLimitDecision decision = limiter.check(request(100));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.retryAfter()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void sendsBaseKeyTimestampWindowAndLimitToLua() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(1L, 99L, now.plusSeconds(60).toEpochMilli(), 0L));

        limiter.check(request(100));

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), argsCaptor.capture());

        assertThat(keysCaptor.getValue()).containsExactly("rl:fixed-window-counter:api-key:test-key");
        assertThat(argsCaptor.getValue()).containsExactly(
                String.valueOf(now.toEpochMilli()),
                String.valueOf(Duration.ofMinutes(1).toMillis()),
                "100"
        );
    }

    private RateLimitRequest request(int limit) {
        return new RateLimitRequest(
                IdentifierType.API_KEY,
                "test-key",
                RateLimitAlgorithm.FIXED_WINDOW_COUNTER,
                limit,
                Duration.ofMinutes(1),
                limit,
                limit / 60.0,
                now,
                null,
                null
        );
    }
}
