package com.himanshu.ratelimiter.annotation;

import com.himanshu.ratelimiter.algorithm.IdentifierType;
import com.himanshu.ratelimiter.algorithm.RateLimitAlgorithm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    int limit();

    long windowSeconds();

    IdentifierType type() default IdentifierType.IP;

    RateLimitAlgorithm algorithm() default RateLimitAlgorithm.TOKEN_BUCKET;

    boolean includeEndpoint() default true;
}
