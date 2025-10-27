package com.paklog.wes.task.adapter.rest.dto;

import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.valueobject.TaskStatus;
import com.paklog.wes.task.domain.valueobject.TaskType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Simplified task response for mobile clients
 */
public record MobileTaskResponse(
        String taskId,
        TaskType type,
        TaskStatus status,
        Priority priority,
        String zone,
        LocationDto location,
        String referenceId,
        Integer estimatedDurationSeconds,
        LocalDateTime deadline,
        Map<String, Object> context,
        boolean isOverdue,
        LocalDateTime acceptedAt,
        LocalDateTime startedAt
) {
    public static MobileTaskResponse fromDomain(WorkTask task) {
        return new MobileTaskResponse(
                task.getTaskId(),
                task.getType(),
                task.getStatus(),
                task.getPriority(),
                task.getZone(),
                LocationDto.fromDomain(task.getTaskLocation()),
                task.getReferenceId(),
                task.getEstimatedDuration() != null ? (int) task.getEstimatedDuration().toSeconds() : null,
                task.getDeadline(),
                task.getContext() != null ? task.getContext().getMetadata() : null,
                task.isOverdue(),
                task.getAcceptedAt(),
                task.getStartedAt()
        );
    }
}
