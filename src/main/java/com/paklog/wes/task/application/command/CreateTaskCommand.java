package com.paklog.wes.task.application.command;

import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.wes.task.domain.entity.TaskContext;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskType;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Command to create a new task
 */
public record CreateTaskCommand(
        TaskType type,
        String warehouseId,
        String zone,
        Location location,
        Priority priority,
        String referenceId,
        Duration estimatedDuration,
        LocalDateTime deadline,
        TaskContext context
) {
}
