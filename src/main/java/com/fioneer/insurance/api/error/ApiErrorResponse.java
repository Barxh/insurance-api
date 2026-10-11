package com.fioneer.insurance.api.error;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public static ApiErrorResponse of(
            HttpStatus status,
            String message,
            String path
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                List.of()
        );
    }

    public static ApiErrorResponse validation(
            String message,
            String path,
            List<FieldError> fieldErrors
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                path,
                fieldErrors
        );
    }

    public record FieldError(String field, String message) {
    }
}
