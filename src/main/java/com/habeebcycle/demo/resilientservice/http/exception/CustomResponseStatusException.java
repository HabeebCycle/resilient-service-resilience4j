package com.habeebcycle.demo.resilientservice.http.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public class CustomResponseStatusException extends ResponseStatusException {

    private final HttpStatus status;
    private final String message;

    public CustomResponseStatusException(HttpStatusCode statusCode, String message) {
        super(statusCode, message);
        this.status = HttpStatus.resolve(statusCode.value());
        this.message = message;
    }


    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
