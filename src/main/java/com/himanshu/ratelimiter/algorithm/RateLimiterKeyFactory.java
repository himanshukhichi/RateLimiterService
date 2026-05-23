package com.himanshu.ratelimiter.algorithm;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class RateLimiterKeyFactory {

    public String keyFor(RateLimitRequest request) {
        String algorithm = request.algorithm().name().toLowerCase(Locale.ROOT).replace('_', '-');
        return "rl:%s:%s:%s".formatted(
                algorithm,
                request.identifierType().keyPrefix(),
                normalizeIdentifier(request.identifier())
        );
    }

    private String normalizeIdentifier(String identifier) {
        return identifier.trim()
                .replaceAll("[^a-zA-Z0-9._:@-]", "_");
    }
}
