package com.himanshu.ratelimiter;

import com.himanshu.ratelimiter.config.RateLimiterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimiterServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RateLimiterServiceApplication.class, args);
    }
}
