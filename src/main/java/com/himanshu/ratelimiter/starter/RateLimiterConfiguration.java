package com.himanshu.ratelimiter.starter;

import com.himanshu.ratelimiter.config.RateLimiterProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RateLimiterProperties.class)
@ComponentScan(basePackages = {
        "com.himanshu.ratelimiter.admin",
        "com.himanshu.ratelimiter.algorithm",
        "com.himanshu.ratelimiter.aop",
        "com.himanshu.ratelimiter.config",
        "com.himanshu.ratelimiter.core",
        "com.himanshu.ratelimiter.filter"
})
public class RateLimiterConfiguration {
}
