package com.himanshu.ratelimiter.admin;

import com.himanshu.ratelimiter.algorithm.IdentifierType;
import com.himanshu.ratelimiter.algorithm.RateLimitAlgorithm;

public record AdminLimitResponse(
        IdentifierType identifierType,
        String identifier,
        String method,
        String endpoint,
        RateLimitAlgorithm algorithm,
        int limit,
        long windowSeconds
) {
}
