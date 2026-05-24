package com.himanshu.ratelimiter.core;

import com.himanshu.ratelimiter.algorithm.IdentifierType;

public record RateLimitTarget(
        IdentifierType identifierType,
        String identifier,
        String method,
        String endpoint
) {
}
