package com.paklog.wes.task.adapter.rest.dto;

import java.time.LocalDateTime;

/**
 * Standard error response for REST API
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, LocalDateTime.now());
    }
}
