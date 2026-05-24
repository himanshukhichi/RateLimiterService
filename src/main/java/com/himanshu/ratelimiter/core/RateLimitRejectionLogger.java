package com.himanshu.ratelimiter.core;

import com.himanshu.ratelimiter.algorithm.RateLimitDecision;
import com.himanshu.ratelimiter.algorithm.IdentifierType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RateLimitRejectionLogger {

    private static final Logger log = LoggerFactory.getLogger(RateLimitRejectionLogger.class);

    public void logRejected(RateLimitDecision decision) {
        log.warn(
                "{\"event\":\"rate_limit_rejected\",\"userId\":\"{}\",\"identifierType\":\"{}\",\"identifier\":\"{}\",\"endpoint\":\"{}\",\"limit\":{},\"windowMs\":{},\"retryAfterMs\":{},\"timestamp\":\"{}\"}",
                userId(decision),
                decision.identifierType(),
                escape(decision.identifier()),
                escape(decision.endpoint()),
                decision.limit(),
                decision.window().toMillis(),
                decision.retryAfter().toMillis(),
                Instant.now()
        );
    }

    private String userId(RateLimitDecision decision) {
        if (decision.identifierType() == IdentifierType.USER_ID) {
            return escape(decision.identifier());
        }
        return "";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
