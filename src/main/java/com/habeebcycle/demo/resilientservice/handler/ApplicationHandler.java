package com.habeebcycle.demo.resilientservice.handler;

import com.habeebcycle.demo.resilientservice.http.exception.CustomResponseStatusException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static com.habeebcycle.demo.resilientservice.util.Constant.CIRCUIT_BREAKER_CONFIG_NAME;
import static com.habeebcycle.demo.resilientservice.util.Constant.RETRY_CONFIG_NAME;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Component
public class ApplicationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationHandler.class);
    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public ApplicationHandler(final WebClient webClient, final CircuitBreakerRegistry circuitBreakerRegistry, final RetryRegistry retryRegistry) {
        this.webClient = webClient;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_CONFIG_NAME);
        this.retry = retryRegistry.retry(RETRY_CONFIG_NAME);
    }

    public Mono<ServerResponse> apiGetRequest(final String path) {
        Mono<String> responseBody = webClient.get()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleErrorResponse)
                .bodyToMono(String.class)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .transformDeferred(RetryOperator.of(retry)) // ORDER - If above, retry will complete before a failure is recorded by the circuit breaker
                .doOnError(CallNotPermittedException.class::isInstance, throwable -> {
                    LOG.error("Circuit Breaker is in [{}]... Providing fallback response without calling the API", circuitBreaker.getState());
                    throw new CustomResponseStatusException(SERVICE_UNAVAILABLE, "API service is unavailable");
                });

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseBody, String.class);
    }

    private Mono<CustomResponseStatusException> handleErrorResponse(final ClientResponse clientResponse) {
        LOG.info("Handling error response: [{}]", clientResponse.statusCode());
        return clientResponse
                .bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(res -> Mono.just(new CustomResponseStatusException(clientResponse.statusCode(), res)));
    }
}
