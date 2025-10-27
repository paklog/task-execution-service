package com.paklog.wes.task.domain.event;

import com.paklog.task.execution.domain.shared.DomainEvent;
import com.paklog.wes.task.domain.valueobject.TaskType;

import java.time.Duration;
import java.time.Instant;

/**
 * Event published when a task is completed successfully
 */
public class TaskCompletedEvent implements DomainEvent {

    private final String taskId;
    private final TaskType type;
    private final String completedBy;
    private final String warehouseId;
    private final String referenceId;
    private final Duration actualDuration;
    private final boolean completedOnTime;
    private final Instant occurredOn;

    public TaskCompletedEvent(
            String taskId,
            TaskType type,
            String completedBy,
            String warehouseId,
            String referenceId,
            Duration actualDuration,
            boolean completedOnTime
    ) {
        this.taskId = taskId;
        this.type = type;
        this.completedBy = completedBy;
        this.warehouseId = warehouseId;
        this.referenceId = referenceId;
        this.actualDuration = actualDuration;
        this.completedOnTime = completedOnTime;
        this.occurredOn = Instant.now();
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskType getType() {
        return type;
    }

    public String getCompletedBy() {
        return completedBy;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public Duration getActualDuration() {
        return actualDuration;
    }

    public boolean isCompletedOnTime() {
        return completedOnTime;
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }

    @Override
    public String eventType() {
        return "TaskCompleted";
    }
}
