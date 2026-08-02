package com.tangent.wrapper;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        boolean success,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors,
        Instant timestamp
) {
    public static ApiErrorResponse of(int status, String error, String message) {
        return new ApiErrorResponse(false, status, error, message, Map.of(), Instant.now());
    }

    public static ApiErrorResponse validation(int status, String error, String message,
                                              Map<String, String> fieldErrors) {
        return new ApiErrorResponse(false, status, error, message, fieldErrors, Instant.now());
    }
}
