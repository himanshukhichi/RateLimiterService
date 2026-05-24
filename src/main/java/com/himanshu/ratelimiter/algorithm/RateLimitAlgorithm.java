package com.himanshu.ratelimiter.algorithm;

public enum RateLimitAlgorithm {
    TOKEN_BUCKET,
    SLIDING_WINDOW_LOG,
    FIXED_WINDOW_COUNTER,
    SLIDING_WINDOW_COUNTER
}
