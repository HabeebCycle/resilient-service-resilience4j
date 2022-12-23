package com.habeebcycle.demo.resilientservice.router;

import com.habeebcycle.demo.resilientservice.http.exception.CustomResponseStatusException;
import com.habeebcycle.demo.resilientservice.handler.ApplicationHandler;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.nest;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Component
public class ApplicationRouter {

    private static final Logger LOG = getLogger(ApplicationRouter.class);
    private static final String ROOT_PATH = "/services";

    @Bean
    @Order(1)
    public RouterFunction<ServerResponse> routerFunction(final ApplicationHandler handler) {
        return nest(path(ROOT_PATH).and(accept(APPLICATION_JSON)),
                route(GET("/posts"), request -> handler.apiGetRequest(request.path().substring(ROOT_PATH.length())))
                        .andRoute(GET("/posts/{id}"), request -> handler.apiGetRequest(request.path().substring(ROOT_PATH.length())))
        );
    }

    @Bean
    @Order(99)
    public RouterFunction<ServerResponse> defaultRouterFunction() {
        return route(path("/**"), this::defaultResponse);
    }

    private @NonNull Mono<ServerResponse> defaultResponse(final ServerRequest serverRequest) {
        final String path = serverRequest.path();
        LOG.info("Requested url or resource {} is not found.", path);
        return Mono.error(new CustomResponseStatusException(NOT_FOUND, "Resource requested is not found."));
    }
}
