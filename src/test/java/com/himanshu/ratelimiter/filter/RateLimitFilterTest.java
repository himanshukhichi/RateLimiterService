package com.himanshu.ratelimiter.filter;

import com.himanshu.ratelimiter.algorithm.IdentifierType;
import com.himanshu.ratelimiter.algorithm.RateLimitAlgorithm;
import com.himanshu.ratelimiter.algorithm.RateLimitDecision;
import com.himanshu.ratelimiter.core.ClientIdentifierResolver;
import com.himanshu.ratelimiter.core.RateLimitEnforcementService;
import com.himanshu.ratelimiter.core.RateLimitRejectionLogger;
import com.himanshu.ratelimiter.core.RateLimitResponseWriter;
import com.himanshu.ratelimiter.core.RateLimitTarget;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    private final Instant now = Instant.parse("2026-05-23T10:15:30Z");
    private RateLimitEnforcementService enforcementService;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        enforcementService = mock(RateLimitEnforcementService.class);
        filter = new RateLimitFilter(
                new ClientIdentifierResolver(),
                enforcementService,
                new RateLimitResponseWriter(),
                new RateLimitRejectionLogger()
        );
    }

    @Test
    void limitsByApiKeyWhenHeaderIsPresent() throws Exception {
        when(enforcementService.check(any())).thenReturn(allowed(IdentifierType.API_KEY, "user-api-key"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ping");
        request.addHeader("X-API-Key", "user-api-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        ArgumentCaptor<RateLimitTarget> captor = ArgumentCaptor.forClass(RateLimitTarget.class);
        verify(enforcementService).check(captor.capture());
        assertThat(captor.getValue().identifierType()).isEqualTo(IdentifierType.API_KEY);
        assertThat(captor.getValue().identifier()).isEqualTo("user-api-key");
        verify(chain).doFilter(request, response);
    }

    @Test
    void fallsBackToFirstForwardedIpWhenApiKeyIsMissing() throws Exception {
        when(enforcementService.check(any())).thenReturn(allowed(IdentifierType.IP, "203.0.113.7"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ping");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.2");
        request.setRemoteAddr("10.0.0.9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        ArgumentCaptor<RateLimitTarget> captor = ArgumentCaptor.forClass(RateLimitTarget.class);
        verify(enforcementService).check(captor.capture());
        assertThat(captor.getValue().identifierType()).isEqualTo(IdentifierType.IP);
        assertThat(captor.getValue().identifier()).isEqualTo("203.0.113.7");
    }

    @Test
    void writesRetryAfterAnd429WhenRejected() throws Exception {
        when(enforcementService.check(any())).thenReturn(new RateLimitDecision(
                false,
                100,
                0,
                now.plusSeconds(5),
                Duration.ofSeconds(5),
                Duration.ofMinutes(1),
                RateLimitAlgorithm.TOKEN_BUCKET,
                IdentifierType.API_KEY,
                "user-api-key",
                null,
                null
        ));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ping");
        request.addHeader("X-API-Key", "user-api-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("5");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getContentAsString()).contains("rate_limit_exceeded");
        verify(chain, never()).doFilter(request, response);
    }

    private RateLimitDecision allowed(IdentifierType identifierType, String identifier) {
        return new RateLimitDecision(
                true,
                100,
                99,
                now.plusSeconds(60),
                Duration.ZERO,
                Duration.ofMinutes(1),
                RateLimitAlgorithm.TOKEN_BUCKET,
                identifierType,
                identifier,
                null,
                null
        );
    }
}
