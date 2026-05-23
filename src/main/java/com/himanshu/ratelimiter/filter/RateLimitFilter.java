package com.himanshu.ratelimiter.filter;

import com.himanshu.ratelimiter.algorithm.IdentifierType;
import com.himanshu.ratelimiter.algorithm.RateLimitDecision;
import com.himanshu.ratelimiter.algorithm.RateLimitRequest;
import com.himanshu.ratelimiter.algorithm.RateLimiterFacade;
import com.himanshu.ratelimiter.config.RateLimiterProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(prefix = "rate-limiter", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    static final String API_KEY_HEADER = "X-API-Key";
    static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final RateLimiterFacade rateLimiterFacade;
    private final RateLimiterProperties properties;
    private final Clock clock;

    public RateLimitFilter(RateLimiterFacade rateLimiterFacade, RateLimiterProperties properties, Clock clock) {
        this.rateLimiterFacade = rateLimiterFacade;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.equals("/error") || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RateLimitRequest rateLimitRequest = buildRateLimitRequest(request);
        RateLimitDecision decision = rateLimiterFacade.check(rateLimitRequest);
        applyRateLimitHeaders(response, decision);

        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"error":"rate_limit_exceeded","message":"Too many requests"}
                """);
    }

    private RateLimitRequest buildRateLimitRequest(HttpServletRequest request) {
        String apiKey = trimToNull(request.getHeader(API_KEY_HEADER));
        IdentifierType identifierType = apiKey == null ? IdentifierType.IP : IdentifierType.API_KEY;
        String identifier = apiKey == null ? clientIp(request) : apiKey;
        RateLimiterProperties.Limit limit = identifierType == IdentifierType.API_KEY
                ? properties.getApiKey()
                : properties.getIp();
        Instant now = Instant.now(clock);

        return new RateLimitRequest(
                identifierType,
                identifier,
                properties.getAlgorithm(),
                limit.getLimit(),
                limit.getWindow(),
                limit.getLimit(),
                refillRatePerSecond(limit),
                now
        );
    }

    private double refillRatePerSecond(RateLimiterProperties.Limit limit) {
        double windowSeconds = Math.max(1.0, limit.getWindow().toMillis() / 1000.0);
        return limit.getLimit() / windowSeconds;
    }

    private void applyRateLimitHeaders(HttpServletResponse response, RateLimitDecision decision) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, decision.remaining())));
        response.setHeader("X-RateLimit-Reset", String.valueOf(decision.resetAt().getEpochSecond()));
        if (!decision.allowed()) {
            long retryAfterSeconds = Math.max(1, decision.retryAfter().toSeconds());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        }
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
