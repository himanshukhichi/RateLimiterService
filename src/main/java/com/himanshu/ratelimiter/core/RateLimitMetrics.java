package com.himanshu.ratelimiter.core;

import com.himanshu.ratelimiter.algorithm.RateLimitDecision;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RateLimitMetrics {

    private final MeterRegistry meterRegistry;

    public RateLimitMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void record(RateLimitDecision decision, Duration duration) {
        String algorithm = decision.algorithm().name();
        String identifierType = decision.identifierType().name();

        meterRegistry.counter(
                decision.allowed() ? "requests_allowed_total" : "requests_rejected_total",
                "algorithm", algorithm,
                "identifier_type", identifierType
        ).increment();

        DistributionSummary.builder("rate_limit_check_duration_ms")
                .description("Time spent checking a rate limit")
                .baseUnit("milliseconds")
                .publishPercentileHistogram()
                .tag("algorithm", algorithm)
                .tag("identifier_type", identifierType)
                .register(meterRegistry)
                .record(duration.toNanos() / 1_000_000.0);
    }
}
