package com.himanshu.ratelimiter.algorithm;

import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class RateLimiterFacade {

    private final Map<RateLimitAlgorithm, RateLimiter> limiters = new EnumMap<>(RateLimitAlgorithm.class);

    public RateLimiterFacade(List<RateLimiter> rateLimiters) {
        for (RateLimiter limiter : rateLimiters) {
            limiters.put(limiter.algorithm(), limiter);
        }
    }

    public RateLimitDecision check(RateLimitRequest request) {
        RateLimiter limiter = limiters.get(request.algorithm());
        if (limiter == null) {
            throw new IllegalArgumentException("Unsupported rate limiting algorithm: " + request.algorithm());
        }
        return limiter.check(request);
    }
}
