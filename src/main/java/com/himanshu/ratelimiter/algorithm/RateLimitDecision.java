package com.himanshu.ratelimiter.algorithm;

import java.time.Duration;
import java.time.Instant;

public record RateLimitDecision(
        boolean allowed,
        int limit,
        long remaining,
        Instant resetAt,
        Duration retryAfter,
        RateLimitAlgorithm algorithm,
        IdentifierType identifierType,
        String identifier
) {
}
