package com.himanshu.ratelimiter.core;

import com.himanshu.ratelimiter.algorithm.RateLimitDecision;

public class RateLimitExceededException extends RuntimeException {

    private final RateLimitDecision decision;

    public RateLimitExceededException(RateLimitDecision decision) {
        super("Rate limit exceeded");
        this.decision = decision;
    }

    public RateLimitDecision decision() {
        return decision;
    }
}
