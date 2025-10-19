package com.paklog.wes.task.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to fail a task
 */
public record FailTaskRequest(
        @NotBlank(message = "Reason is required")
        String reason
) {
}
