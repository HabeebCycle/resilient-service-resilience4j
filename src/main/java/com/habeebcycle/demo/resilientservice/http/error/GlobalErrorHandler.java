package com.habeebcycle.demo.resilientservice.http.error;

import com.habeebcycle.demo.resilientservice.http.exception.CustomResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GlobalErrorHandler extends AbstractErrorWebExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalErrorHandler.class);

    public GlobalErrorHandler(final ErrorAttributes errorAttributes, final ApplicationContext applicationContext,
                              final ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, new WebProperties.Resources(), applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private @NonNull Mono<ServerResponse> renderErrorResponse(final ServerRequest serverRequest) {

        final Throwable error = getError(serverRequest);
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        int statusCode = 500;

        if (error instanceof CustomResponseStatusException) {
            httpStatus = ((CustomResponseStatusException) error).getStatus();
            statusCode = ((CustomResponseStatusException) error).getStatusCode().value();
        }

        return ServerResponse.status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createHttpErrorInfo(statusCode, httpStatus, serverRequest.path(), error.getMessage()));
    }

    private ErrorInfo createHttpErrorInfo(final int status, final HttpStatus httpStatus, final String path, final String message) {
        LOG.debug("Returning HTTP Error status: [{}] for path: [{}], message: [{}]", httpStatus, path, message);
        return new ErrorInfo(path, status, httpStatus, message);
    }
}


