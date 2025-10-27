package com.paklog.wes.task.adapter.rest.dto;

import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.wes.task.domain.valueobject.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Request to create a new task
 */
public record CreateTaskRequest(
        @NotNull(message = "Task type is required")
        TaskType type,

        @NotBlank(message = "Warehouse ID is required")
        String warehouseId,

        String zone,

        LocationDto location,

        Priority priority,

        @NotBlank(message = "Reference ID is required")
        String referenceId,

        Integer estimatedDurationSeconds,

        LocalDateTime deadline,

        @NotNull(message = "Task context is required")
        Map<String, Object> context
) {
}
