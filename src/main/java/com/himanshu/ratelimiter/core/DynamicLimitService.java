package com.himanshu.ratelimiter.core;

import com.himanshu.ratelimiter.admin.AdminLimitRequest;
import com.himanshu.ratelimiter.algorithm.IdentifierType;
import com.himanshu.ratelimiter.algorithm.RateLimitAlgorithm;
import com.himanshu.ratelimiter.config.RateLimiterProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class DynamicLimitService {

    private static final String LIMITS_HASH = "rl:config:limits";

    private final StringRedisTemplate redisTemplate;
    private final RateLimiterProperties properties;

    public DynamicLimitService(StringRedisTemplate redisTemplate, RateLimiterProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public LimitRule resolve(RateLimitTarget target) {
        return lookup(target)
                .orElseGet(() -> defaultRule(target.identifierType()));
    }

    public LimitRule save(AdminLimitRequest request) {
        LimitRule rule = new LimitRule(
                request.algorithm() == null ? properties.getAlgorithm() : request.algorithm(),
                request.limit(),
                Duration.ofSeconds(request.windowSeconds())
        );
        redisTemplate.opsForHash().put(LIMITS_HASH, fieldName(request), encode(rule));
        return rule;
    }

    private Optional<LimitRule> lookup(RateLimitTarget target) {
        List<String> candidates = List.of(
                fieldName(target.identifierType(), target.identifier(), target.method(), target.endpoint()),
                fieldName(target.identifierType(), target.identifier(), target.method(), null),
                fieldName(target.identifierType(), target.identifier(), null, null),
                fieldName(target.identifierType(), "*", target.method(), target.endpoint()),
                fieldName(target.identifierType(), "*", target.method(), null),
                fieldName(target.identifierType(), "*", null, null)
        );

        for (String candidate : candidates) {
            Object raw = redisTemplate.opsForHash().get(LIMITS_HASH, candidate);
            if (raw != null) {
                return Optional.of(decode(raw.toString()));
            }
        }
        return Optional.empty();
    }

    private LimitRule defaultRule(IdentifierType identifierType) {
        RateLimiterProperties.Limit limit = identifierType == IdentifierType.IP
                ? properties.getIp()
                : properties.getApiKey();
        return new LimitRule(properties.getAlgorithm(), limit.getLimit(), limit.getWindow());
    }

    private String fieldName(AdminLimitRequest request) {
        return fieldName(request.identifierType(), request.identifier(), request.method(), request.endpoint());
    }

    private String fieldName(IdentifierType identifierType, String identifier, String method, String endpoint) {
        return "%s:%s:%s:%s".formatted(
                identifierType.name(),
                normalize(identifier == null || identifier.isBlank() ? "*" : identifier),
                normalize(method == null || method.isBlank() ? "*" : method.toUpperCase(Locale.ROOT)),
                normalize(endpoint == null || endpoint.isBlank() ? "*" : endpoint)
        );
    }

    private String encode(LimitRule rule) {
        return "%s,%d,%d".formatted(rule.algorithm(), rule.limit(), rule.window().toSeconds());
    }

    private LimitRule decode(String value) {
        String[] parts = value.split(",");
        return new LimitRule(
                RateLimitAlgorithm.valueOf(parts[0]),
                Integer.parseInt(parts[1]),
                Duration.ofSeconds(Long.parseLong(parts[2]))
        );
    }

    private String normalize(String value) {
        return value.trim().replaceAll("[^a-zA-Z0-9._:@*\\-/]", "_");
    }
}
