package com.himanshu.ratelimiter.algorithm;

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
class SlidingWindowLogRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private SlidingWindowLogRateLimiter limiter;
    private final Instant now = Instant.parse("2026-05-23T10:15:30Z");

    @BeforeEach
    void setUp() {
        limiter = new SlidingWindowLogRateLimiter(redisTemplate, new RateLimiterKeyFactory());
    }

    @Test
    void allowsRequestAtBoundaryBeforeLimitIsReached() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(1L, 0L, now.plusSeconds(60).toEpochMilli(), 0L));

        RateLimitDecision decision = limiter.check(request(100));

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.limit()).isEqualTo(100);
        assertThat(decision.remaining()).isZero();
    }

    @Test
    void rejectsRequestWhenWindowIsAlreadyAtLimit() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(0L, 0L, now.plusSeconds(2).toEpochMilli(), 2_000L));

        RateLimitDecision decision = limiter.check(request(100));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.retryAfter()).isEqualTo(Duration.ofSeconds(2));
        assertThat(decision.resetAt()).isEqualTo(now.plusSeconds(2));
    }

    @Test
    void sendsWindowLimitAndUniqueMemberToLuaInOneRedisCall() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(List.of(1L, 99L, now.plusSeconds(60).toEpochMilli(), 0L));

        limiter.check(request(100));

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(any(RedisScript.class), keysCaptor.capture(), argsCaptor.capture());

        assertThat(keysCaptor.getValue()).containsExactly("rl:sliding-window-log:ip:203.0.113.10");
        assertThat(argsCaptor.getValue()[0]).isEqualTo(String.valueOf(now.toEpochMilli()));
        assertThat(argsCaptor.getValue()[1]).isEqualTo(String.valueOf(Duration.ofMinutes(1).toMillis()));
        assertThat(argsCaptor.getValue()[2]).isEqualTo("100");
        assertThat(argsCaptor.getValue()[3].toString()).startsWith(now.toEpochMilli() + ":");
    }

    private RateLimitRequest request(int limit) {
        return new RateLimitRequest(
                IdentifierType.IP,
                "203.0.113.10",
                RateLimitAlgorithm.SLIDING_WINDOW_LOG,
                limit,
                Duration.ofMinutes(1),
                limit,
                1.6667,
                now
        );
    }
}
