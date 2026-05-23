package com.himanshu.ratelimiter.algorithm;

public interface RateLimiter {

    RateLimitAlgorithm algorithm();

    RateLimitDecision check(RateLimitRequest request);
}
