package com.paklog.wes.task.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to assign a task to a worker
 */
public record AssignTaskRequest(
        @NotBlank(message = "Worker ID is required")
        String workerId,

        boolean force
) {
}
