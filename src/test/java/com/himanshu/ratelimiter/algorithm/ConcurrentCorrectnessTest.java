package com.himanshu.ratelimiter.algorithm;

import com.himanshu.ratelimiter.config.RateLimiterProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "redis.integration.enabled", matches = "true")
class ConcurrentCorrectnessTest {

    private LettuceConnectionFactory connectionFactory;

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void tokenBucketAllowsExactlyLimitRequestsUnderConcurrentLoad() throws Exception {
        int limit = 25;
        int threads = 100;
        String identifier = "concurrency-" + UUID.randomUUID();
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(redisTemplate(), keyFactory());
        RateLimitRequest request = new RateLimitRequest(
                IdentifierType.API_KEY,
                identifier,
                RateLimitAlgorithm.TOKEN_BUCKET,
                limit,
                Duration.ofHours(1),
                limit,
                0.000001,
                Instant.now(),
                null,
                null
        );
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                return limiter.check(request).allowed();
            }));
        }

        ready.await();
        start.countDown();

        long allowed = 0;
        for (Future<Boolean> future : futures) {
            if (future.get()) {
                allowed++;
            }
        }
        executor.shutdown();

        assertThat(allowed).isEqualTo(limit);
    }

    private StringRedisTemplate redisTemplate() {
        connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    private RateLimiterKeyFactory keyFactory() {
        return new RateLimiterKeyFactory(new RateLimiterProperties());
    }
}
