package com.paklog.wes.task.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to reject a task assignment
 */
public record RejectTaskRequest(
        @NotBlank(message = "Reason is required")
        String reason
) {
}
