package com.habeebcycle.demo.resilientservice.http.web;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static reactor.core.publisher.Mono.just;

@Component
public class ApplicationWebClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationWebClient.class);

    private final String serverBaseUrl;
    private final int connectTimeout;
    private final int readTimeout;
    private final int writeTimeout;
    private final int maxInMemorySize;


    public ApplicationWebClient(@Value("${api.client.baseUrl}")String serverBaseUrl,
                                @Value("${api.client.connectTimeout}") int connectTimeout,
                                @Value("${api.client.readTimeout}") int readTimeout,
                                @Value("${api.client.writeTimeout}") int writeTimeout,
                                @Value("${api.client.maxInMemorySize:2048}") int maxInMemorySize) {
        this.serverBaseUrl = serverBaseUrl;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
        this.maxInMemorySize = maxInMemorySize;
    }

    @Bean
    public WebClient apiGatewayWebClient(final WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(config -> config.defaultCodecs().maxInMemorySize(maxInMemorySize)).build())
                .filter(logRequestDetails())
                .filter(logResponseDetails())
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .defaultHeader(ACCEPT, APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(clientConnectorConfig()))
                .baseUrl(serverBaseUrl)
                .build();
    }

    private ExchangeFilterFunction logRequestDetails() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            LOGGER.info("Sending [{}] request to URL [{}] with request headers [{}]",
                    clientRequest.method(), clientRequest.url(), clientRequest.headers());
            return just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponseDetails() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse ->
                clientResponse.bodyToMono(String.class).defaultIfEmpty("").flatMap(responseBody -> {
                    final ClientResponse orgClientResponse = clientResponse.mutate().body(responseBody).build();
                    LOGGER.info("Received response from API with body [{}] status [{}] with response headers [{}]",
                            responseBody, clientResponse.statusCode(), clientResponse.headers().asHttpHeaders().toSingleValueMap());
                    return just(orgClientResponse);
                })
        );
    }

    private HttpClient clientConnectorConfig() {
        return HttpClient.create().option(CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, MILLISECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(writeTimeout, MILLISECONDS));
                });
    }
}
