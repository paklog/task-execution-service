package com.paklog.wes.task.domain.event;

import com.paklog.task.execution.domain.shared.DomainEvent;
import com.paklog.wes.task.domain.valueobject.TaskType;

import java.time.Instant;

/**
 * Event published when a task fails
 */
public class TaskFailedEvent implements DomainEvent {

    private final String taskId;
    private final TaskType type;
    private final String failedBy;
    private final String warehouseId;
    private final String referenceId;
    private final String failureReason;
    private final Instant occurredOn;

    public TaskFailedEvent(
            String taskId,
            TaskType type,
            String failedBy,
            String warehouseId,
            String referenceId,
            String failureReason
    ) {
        this.taskId = taskId;
        this.type = type;
        this.failedBy = failedBy;
        this.warehouseId = warehouseId;
        this.referenceId = referenceId;
        this.failureReason = failureReason;
        this.occurredOn = Instant.now();
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskType getType() {
        return type;
    }

    public String getFailedBy() {
        return failedBy;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }

    @Override
    public String eventType() {
        return "TaskFailed";
    }
}
