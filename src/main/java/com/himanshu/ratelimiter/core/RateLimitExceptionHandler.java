package com.himanshu.ratelimiter.core;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@RestControllerAdvice
public class RateLimitExceptionHandler {

    private final RateLimitResponseWriter responseWriter;
    private final RateLimitRejectionLogger rejectionLogger;

    public RateLimitExceptionHandler(
            RateLimitResponseWriter responseWriter,
            RateLimitRejectionLogger rejectionLogger
    ) {
        this.responseWriter = responseWriter;
        this.rejectionLogger = rejectionLogger;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    void handle(RateLimitExceededException exception, HttpServletResponse response) throws IOException {
        rejectionLogger.logRejected(exception.decision());
        responseWriter.writeRejected(response, exception.decision());
    }
}
