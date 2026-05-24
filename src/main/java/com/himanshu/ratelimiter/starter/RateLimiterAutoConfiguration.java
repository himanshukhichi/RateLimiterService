package com.himanshu.ratelimiter.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(RateLimiterConfiguration.class)
public class RateLimiterAutoConfiguration {
}
