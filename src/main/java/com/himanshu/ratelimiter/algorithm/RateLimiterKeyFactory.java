package com.himanshu.ratelimiter.algorithm;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;

@Component
public class RateLimiterKeyFactory {

    private final com.himanshu.ratelimiter.config.RateLimiterProperties properties;

    public RateLimiterKeyFactory(com.himanshu.ratelimiter.config.RateLimiterProperties properties) {
        this.properties = properties;
    }

    public String keyFor(RateLimitRequest request) {
        String algorithm = request.algorithm().name().toLowerCase(Locale.ROOT).replace('_', '-');
        String identifier = normalizeIdentifier(request.identifier());
        String endpoint = endpointPart(request);
        String clusterTag = properties.isRedisClusterMode() ? "{%s}".formatted(identifier) : identifier;
        return "rl:%s:%s:%s".formatted(
                algorithm,
                request.identifierType().keyPrefix(),
                clusterTag
        ) + endpoint;
    }

    private String endpointPart(RateLimitRequest request) {
        if (request.method() == null || request.endpoint() == null) {
            return "";
        }
        String method = normalizeIdentifier(request.method().toUpperCase(Locale.ROOT));
        String endpoint = normalizeIdentifier(request.endpoint());
        return ":" + method + ":" + endpoint;
    }

    private String normalizeIdentifier(String identifier) {
        return Objects.requireNonNull(identifier, "identifier must not be null")
                .trim()
                .replaceAll("[^a-zA-Z0-9._:@-]", "_");
    }
}
