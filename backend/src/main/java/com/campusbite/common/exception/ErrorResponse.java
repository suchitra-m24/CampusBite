package com.campusbite.common.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(int status, String errorCode, String message) {
        return new ErrorResponse(Instant.now(), status, errorCode, message, null);
    }

    public static ErrorResponse of(int status, String errorCode, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, errorCode, message, fieldErrors);
    }
}
