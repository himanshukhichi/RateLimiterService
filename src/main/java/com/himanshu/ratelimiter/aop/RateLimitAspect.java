package com.himanshu.ratelimiter.aop;

import com.himanshu.ratelimiter.algorithm.RateLimitDecision;
import com.himanshu.ratelimiter.annotation.RateLimit;
import com.himanshu.ratelimiter.core.ClientIdentifierResolver;
import com.himanshu.ratelimiter.core.LimitRule;
import com.himanshu.ratelimiter.core.RateLimitEnforcementService;
import com.himanshu.ratelimiter.core.RateLimitExceededException;
import com.himanshu.ratelimiter.core.RateLimitTarget;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Aspect
@Component
@ConditionalOnProperty(prefix = "rate-limiter", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAspect {

    private final ClientIdentifierResolver identifierResolver;
    private final RateLimitEnforcementService enforcementService;

    public RateLimitAspect(
            ClientIdentifierResolver identifierResolver,
            RateLimitEnforcementService enforcementService
    ) {
        this.identifierResolver = identifierResolver;
        this.enforcementService = enforcementService;
    }

    @Around("@annotation(rateLimit)")
    public Object enforce(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = currentRequest();
        RateLimitTarget target = identifierResolver.target(
                rateLimit.type(),
                request,
                rateLimit.includeEndpoint()
        );
        LimitRule rule = new LimitRule(
                rateLimit.algorithm(),
                rateLimit.limit(),
                Duration.ofSeconds(rateLimit.windowSeconds())
        );
        RateLimitDecision decision = enforcementService.check(target, rule);
        if (!decision.allowed()) {
            throw new RateLimitExceededException(decision);
        }
        return joinPoint.proceed();
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("@RateLimit can only be used inside an HTTP request");
        }
        return attributes.getRequest();
    }
}
