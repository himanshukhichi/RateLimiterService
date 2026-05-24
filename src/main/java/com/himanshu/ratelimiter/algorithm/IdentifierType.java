package com.himanshu.ratelimiter.algorithm;

public enum IdentifierType {
    API_KEY("api-key"),
    IP("ip"),
    USER_ID("user-id");

    private final String keyPrefix;

    IdentifierType(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String keyPrefix() {
        return keyPrefix;
    }
}
