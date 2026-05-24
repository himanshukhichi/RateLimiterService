package com.himanshu.ratelimiter.core;

import com.himanshu.ratelimiter.admin.AdminLimitRequest;
import com.himanshu.ratelimiter.algorithm.IdentifierType;
import com.himanshu.ratelimiter.algorithm.RateLimitAlgorithm;
import com.himanshu.ratelimiter.config.RateLimiterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private DynamicLimitService dynamicLimitService;

    @BeforeEach
    void setUp() {
        RateLimiterProperties properties = new RateLimiterProperties();
        properties.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        properties.getIp().setLimit(100);
        properties.getIp().setWindow(Duration.ofMinutes(1));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        dynamicLimitService = new DynamicLimitService(redisTemplate, properties);
    }

    @Test
    void savesDynamicLimitInRedisHash() {
        AdminLimitRequest request = new AdminLimitRequest(
                IdentifierType.USER_ID,
                "user-123",
                "POST",
                "/api/checkout",
                RateLimitAlgorithm.SLIDING_WINDOW_LOG,
                10,
                30
        );

        LimitRule rule = dynamicLimitService.save(request);

        assertThat(rule.algorithm()).isEqualTo(RateLimitAlgorithm.SLIDING_WINDOW_LOG);
        assertThat(rule.limit()).isEqualTo(10);
        assertThat(rule.window()).isEqualTo(Duration.ofSeconds(30));
        verify(hashOperations).put(
                "rl:config:limits",
                "USER_ID:user-123:POST:/api/checkout",
                "SLIDING_WINDOW_LOG,10,30"
        );
    }

    @Test
    void resolvesMostSpecificDynamicLimitBeforeDefaults() {
        when(hashOperations.get("rl:config:limits", "USER_ID:user-123:POST:/api/checkout"))
                .thenReturn("TOKEN_BUCKET,5,60");
        RateLimitTarget target = new RateLimitTarget(
                IdentifierType.USER_ID,
                "user-123",
                "POST",
                "/api/checkout"
        );

        LimitRule rule = dynamicLimitService.resolve(target);

        assertThat(rule.algorithm()).isEqualTo(RateLimitAlgorithm.TOKEN_BUCKET);
        assertThat(rule.limit()).isEqualTo(5);
        assertThat(rule.window()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void fallsBackToConfiguredDefaultsWhenNoDynamicRuleExists() {
        RateLimitTarget target = new RateLimitTarget(IdentifierType.IP, "203.0.113.10", null, null);

        LimitRule rule = dynamicLimitService.resolve(target);

        assertThat(rule.algorithm()).isEqualTo(RateLimitAlgorithm.TOKEN_BUCKET);
        assertThat(rule.limit()).isEqualTo(100);
        assertThat(rule.window()).isEqualTo(Duration.ofMinutes(1));
    }
}
