package com.paklog.wes.task.adapter.rest.dto;

import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.valueobject.TaskStatus;
import com.paklog.wes.task.domain.valueobject.TaskType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Task response DTO
 */
public record TaskResponse(
        String taskId,
        TaskType type,
        TaskStatus status,
        Priority priority,
        String assignedTo,
        String warehouseId,
        String zone,
        LocationDto location,
        String referenceId,
        Integer estimatedDurationSeconds,
        Integer actualDurationSeconds,
        LocalDateTime deadline,
        LocalDateTime createdAt,
        LocalDateTime queuedAt,
        LocalDateTime assignedAt,
        LocalDateTime acceptedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Map<String, Object> context,
        String failureReason,
        String cancellationReason,
        boolean isOverdue
) {
    public static TaskResponse fromDomain(WorkTask task) {
        return new TaskResponse(
                task.getTaskId(),
                task.getType(),
                task.getStatus(),
                task.getPriority(),
                task.getAssignedTo(),
                task.getWarehouseId(),
                task.getZone(),
                LocationDto.fromDomain(task.getTaskLocation()),
                task.getReferenceId(),
                task.getEstimatedDuration() != null ? (int) task.getEstimatedDuration().toSeconds() : null,
                task.getActualDuration() != null ? (int) task.getActualDuration().toSeconds() : null,
                task.getDeadline(),
                task.getCreatedAt(),
                task.getQueuedAt(),
                task.getAssignedAt(),
                task.getAcceptedAt(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getContext() != null ? task.getContext().getMetadata() : null,
                task.getFailureReason(),
                task.getCancellationReason(),
                task.isOverdue()
        );
    }
}
