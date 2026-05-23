package com.himanshu.ratelimiter.algorithm;

import java.time.Duration;
import java.time.Instant;

public record RateLimitRequest(
        IdentifierType identifierType,
        String identifier,
        RateLimitAlgorithm algorithm,
        int limit,
        Duration window,
        int tokenBucketCapacity,
        double tokenBucketRefillRatePerSecond,
        Instant now
) {
}
