package com.habeebcycle.demo.resilientservice.http.error;

import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;

public class ErrorInfo {

    private final ZonedDateTime timestamp;
    private final String path;
    private final int statusCode;
    private final HttpStatus status;
    private final String message;

    public ErrorInfo(final String path, final int statusCode, final HttpStatus status, final String message) {
        this.timestamp = ZonedDateTime.now();
        this.path = path;
        this.statusCode = statusCode;
        this.status = status;
        this.message = message;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getPath() {
        return path;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
