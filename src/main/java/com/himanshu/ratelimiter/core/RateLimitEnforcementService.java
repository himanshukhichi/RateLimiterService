package com.himanshu.ratelimiter.core;

import com.himanshu.ratelimiter.algorithm.RateLimitDecision;
import com.himanshu.ratelimiter.algorithm.RateLimitRequest;
import com.himanshu.ratelimiter.algorithm.RateLimiterFacade;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class RateLimitEnforcementService {

    private final RateLimiterFacade rateLimiterFacade;
    private final DynamicLimitService dynamicLimitService;
    private final RateLimitMetrics metrics;
    private final Clock clock;

    public RateLimitEnforcementService(
            RateLimiterFacade rateLimiterFacade,
            DynamicLimitService dynamicLimitService,
            RateLimitMetrics metrics,
            Clock clock
    ) {
        this.rateLimiterFacade = rateLimiterFacade;
        this.dynamicLimitService = dynamicLimitService;
        this.metrics = metrics;
        this.clock = clock;
    }

    public RateLimitDecision check(RateLimitTarget target) {
        return check(target, dynamicLimitService.resolve(target));
    }

    public RateLimitDecision check(RateLimitTarget target, LimitRule rule) {
        Instant start = Instant.now(clock);
        RateLimitDecision decision = rateLimiterFacade.check(toRequest(target, rule, start));
        metrics.record(decision, Duration.between(start, Instant.now(clock)));
        return decision;
    }

    private RateLimitRequest toRequest(RateLimitTarget target, LimitRule rule, Instant now) {
        return new RateLimitRequest(
                target.identifierType(),
                target.identifier(),
                rule.algorithm(),
                rule.limit(),
                rule.window(),
                rule.limit(),
                refillRatePerSecond(rule),
                now,
                target.method(),
                target.endpoint()
        );
    }

    private double refillRatePerSecond(LimitRule rule) {
        double windowSeconds = Math.max(1.0, rule.window().toMillis() / 1000.0);
        return rule.limit() / windowSeconds;
    }
}
