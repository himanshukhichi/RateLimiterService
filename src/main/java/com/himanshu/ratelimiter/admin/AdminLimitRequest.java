package com.himanshu.ratelimiter.admin;

import com.himanshu.ratelimiter.algorithm.IdentifierType;
import com.himanshu.ratelimiter.algorithm.RateLimitAlgorithm;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminLimitRequest(
        @NotNull IdentifierType identifierType,
        String identifier,
        String method,
        String endpoint,
        RateLimitAlgorithm algorithm,
        @Min(1) int limit,
        @Min(1) long windowSeconds
) {
}
