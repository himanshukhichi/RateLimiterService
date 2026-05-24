package com.himanshu.ratelimiter.web;

import com.himanshu.ratelimiter.algorithm.IdentifierType;
import com.himanshu.ratelimiter.annotation.RateLimit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DemoController {

    @GetMapping("/ping")
    Map<String, Object> ping() {
        return Map.of(
                "message", "pong",
                "timestamp", Instant.now().toString()
        );
    }

    @PostMapping("/checkout")
    @RateLimit(limit = 20, windowSeconds = 60, type = IdentifierType.USER_ID, includeEndpoint = true)
    Map<String, String> checkout() {
        return Map.of("status", "accepted");
    }
}
