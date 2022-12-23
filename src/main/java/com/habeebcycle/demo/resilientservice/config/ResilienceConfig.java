package com.habeebcycle.demo.resilientservice.config;

import com.habeebcycle.demo.resilientservice.http.exception.CustomResponseStatusException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.util.IllegalReferenceCountException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.habeebcycle.demo.resilientservice.util.Constant.CIRCUIT_BREAKER_CONFIG_NAME;
import static com.habeebcycle.demo.resilientservice.util.Constant.RETRY_CONFIG_NAME;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

//@Configuration
public class ResilienceConfig {

    //@Bean
    public CircuitBreakerRegistry configureCircuitBreakerRegistry() {
        final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                //.slidingWindow(10, 4, COUNT_BASED)
                .slidingWindowSize(10)
                .slidingWindowType(COUNT_BASED)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .slowCallRateThreshold(100)
                .slowCallDurationThreshold(Duration.ofMillis(30000))
                .waitDurationInOpenState(Duration.ofMillis(10000))
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                //.recordException(e -> e instanceof CustomResponseStatusException exception
                        //&& exception.getStatus() == INTERNAL_SERVER_ERROR)
                //.recordExceptions(IOException.class, TimeoutException.class)
                //.recordExceptions(WriteTimeoutException.class, ReadTimeoutException.class, ConnectTimeoutException.class)
                .build();

        return CircuitBreakerRegistry.of(Map.of(CIRCUIT_BREAKER_CONFIG_NAME, circuitBreakerConfig));
    }

    //@Bean
    public RetryRegistry configureRetryRegistry() {
        final RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(5000))
                //.intervalFunction(IntervalFunction.ofExponentialBackoff(IntervalFunction.DEFAULT_INITIAL_INTERVAL, 2))
                .build();

        return RetryRegistry.of(Map.of(RETRY_CONFIG_NAME, retryConfig));
    }
}
