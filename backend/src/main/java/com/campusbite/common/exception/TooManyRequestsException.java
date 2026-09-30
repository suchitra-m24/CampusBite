package com.campusbite.common.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends ApiException {
    public TooManyRequestsException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED");
    }
}
