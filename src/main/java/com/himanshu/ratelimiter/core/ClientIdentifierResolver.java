package com.himanshu.ratelimiter.core;

import com.himanshu.ratelimiter.algorithm.IdentifierType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ClientIdentifierResolver {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    public static final String USER_ID_HEADER = "X-User-Id";

    public RateLimitTarget defaultTarget(HttpServletRequest request) {
        String apiKey = trimToNull(request.getHeader(API_KEY_HEADER));
        if (apiKey != null) {
            return target(IdentifierType.API_KEY, apiKey, request, false);
        }
        return target(IdentifierType.IP, clientIp(request), request, false);
    }

    public RateLimitTarget target(
            IdentifierType identifierType,
            HttpServletRequest request,
            boolean includeEndpoint
    ) {
        return target(identifierType, identifier(identifierType, request), request, includeEndpoint);
    }

    private RateLimitTarget target(
            IdentifierType identifierType,
            String identifier,
            HttpServletRequest request,
            boolean includeEndpoint
    ) {
        String method = includeEndpoint ? request.getMethod() : null;
        String endpoint = includeEndpoint ? request.getRequestURI() : null;
        return new RateLimitTarget(identifierType, identifier, method, endpoint);
    }

    private String identifier(IdentifierType identifierType, HttpServletRequest request) {
        return switch (identifierType) {
            case API_KEY -> requireIdentifier(identifierType, request.getHeader(API_KEY_HEADER));
            case IP -> clientIp(request);
            case USER_ID -> requireIdentifier(identifierType, request.getHeader(USER_ID_HEADER));
        };
    }

    private String requireIdentifier(IdentifierType identifierType, String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException("Missing identifier header for " + identifierType);
        }
        return trimmed;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = trimToNull(request.getHeader(FORWARDED_FOR_HEADER));
        if (forwardedFor != null) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
