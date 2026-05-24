package com.himanshu.ratelimiter.filter;

import com.himanshu.ratelimiter.algorithm.RateLimitDecision;
import com.himanshu.ratelimiter.core.ClientIdentifierResolver;
import com.himanshu.ratelimiter.core.RateLimitEnforcementService;
import com.himanshu.ratelimiter.core.RateLimitRejectionLogger;
import com.himanshu.ratelimiter.core.RateLimitResponseWriter;
import com.himanshu.ratelimiter.core.RateLimitTarget;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(prefix = "rate-limiter", name = {"enabled", "filter-enabled"}, havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private final ClientIdentifierResolver identifierResolver;
    private final RateLimitEnforcementService enforcementService;
    private final RateLimitResponseWriter responseWriter;
    private final RateLimitRejectionLogger rejectionLogger;

    public RateLimitFilter(
            ClientIdentifierResolver identifierResolver,
            RateLimitEnforcementService enforcementService,
            RateLimitResponseWriter responseWriter,
            RateLimitRejectionLogger rejectionLogger
    ) {
        this.identifierResolver = identifierResolver;
        this.enforcementService = enforcementService;
        this.responseWriter = responseWriter;
        this.rejectionLogger = rejectionLogger;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/admin")
                || path.equals("/error")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RateLimitTarget target = identifierResolver.defaultTarget(request);
        RateLimitDecision decision = enforcementService.check(target);
        responseWriter.applyHeaders(response, decision);

        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        rejectionLogger.logRejected(decision);
        responseWriter.writeRejected(response, decision);
    }
}
