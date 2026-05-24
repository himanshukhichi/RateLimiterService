package com.himanshu.ratelimiter.core;

import com.himanshu.ratelimiter.algorithm.RateLimitAlgorithm;

import java.time.Duration;

public record LimitRule(
        RateLimitAlgorithm algorithm,
        int limit,
        Duration window
) {
}
