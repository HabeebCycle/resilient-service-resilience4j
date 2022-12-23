package com.habeebcycle.demo.resilientservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import static com.habeebcycle.demo.resilientservice.util.Constant.CIRCUIT_BREAKER_CONFIG_NAME;
import static com.habeebcycle.demo.resilientservice.util.Constant.RETRY_CONFIG_NAME;

@Configuration
public class EventsLoggerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventsLoggerConfig.class);

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public EventsLoggerConfig(CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_CONFIG_NAME);
        this.retry = retryRegistry.retry(RETRY_CONFIG_NAME);
    }

    @PostConstruct
    public void logRegistryEvents() {
        retry.getEventPublisher()
                .onRetry(event -> LOGGER.info("RetryRegistryEventListener: [{}]", event));

        circuitBreaker.getEventPublisher()
                .onCallNotPermitted(event -> LOGGER.info("CircuitBreakerRegistryEventListener: State: [{}] Details: [{}]", circuitBreaker.getState(), event))
                .onSuccess(event -> LOGGER.info("CircuitBreakerRegistryEventListener: onSuccess: [{}] - State: [{}]", event.getEventType(), circuitBreaker.getState()))
                .onError(event -> LOGGER.info("CircuitBreakerRegistryEventListener: onError: [{}]", event))
                .onIgnoredError(event -> LOGGER.info("CircuitBreakerRegistryEventListener: onIgnoredError: [{}] - State: [{}]", event.getEventType(), circuitBreaker.getState()))
                .onReset(event -> LOGGER.info("CircuitBreakerRegistryEventListener: onReset: [{}] - State: [{}]", event.getEventType(), circuitBreaker.getState()))
                .onStateTransition(event -> LOGGER.info("CircuitBreakerRegistryEventListener: onStateTransition: [{}] - State: [{}]", event.getEventType(), circuitBreaker.getState()));
    }
}
