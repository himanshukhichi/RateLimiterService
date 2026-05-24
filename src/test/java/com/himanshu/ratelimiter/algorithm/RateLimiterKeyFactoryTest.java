package com.himanshu.ratelimiter.algorithm;

import com.himanshu.ratelimiter.config.RateLimiterProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterKeyFactoryTest {

    @Test
    void buildsCompositeEndpointKey() {
        RateLimiterKeyFactory keyFactory = new RateLimiterKeyFactory(new RateLimiterProperties());

        String key = keyFactory.keyFor(request("user-123", false));

        assertThat(key).isEqualTo("rl:token-bucket:user-id:user-123:POST:_api_checkout");
    }

    @Test
    void addsRedisClusterHashTagAroundIdentifierWhenClusterModeIsEnabled() {
        RateLimiterProperties properties = new RateLimiterProperties();
        properties.setRedisClusterMode(true);
        RateLimiterKeyFactory keyFactory = new RateLimiterKeyFactory(properties);

        String key = keyFactory.keyFor(request("user-123", true));

        assertThat(key).isEqualTo("rl:token-bucket:user-id:{user-123}:POST:_api_checkout");
    }

    private RateLimitRequest request(String identifier, boolean clusterMode) {
        return new RateLimitRequest(
                IdentifierType.USER_ID,
                identifier,
                RateLimitAlgorithm.TOKEN_BUCKET,
                20,
                Duration.ofSeconds(60),
                20,
                20.0 / 60.0,
                Instant.parse("2026-05-23T10:15:30Z"),
                "POST",
                "/api/checkout"
        );
    }
}
