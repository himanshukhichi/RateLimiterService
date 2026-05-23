package com.himanshu.ratelimiter.config;

import com.himanshu.ratelimiter.algorithm.RateLimitAlgorithm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    private boolean enabled = true;
    private RateLimitAlgorithm algorithm = RateLimitAlgorithm.TOKEN_BUCKET;
    @Valid
    private Limit apiKey = new Limit();
    @Valid
    private Limit ip = new Limit();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RateLimitAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(RateLimitAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public Limit getApiKey() {
        return apiKey;
    }

    public void setApiKey(Limit apiKey) {
        this.apiKey = apiKey;
    }

    public Limit getIp() {
        return ip;
    }

    public void setIp(Limit ip) {
        this.ip = ip;
    }

    public static class Limit {
        @Min(1)
        private int limit = 100;
        private Duration window = Duration.ofMinutes(1);

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }

}
