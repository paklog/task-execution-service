package com.paklog.wes.task.domain.event;

import com.paklog.domain.shared.DomainEvent;
import com.paklog.wes.task.domain.valueobject.TaskType;

import java.time.Duration;

/**
 * Event published when a task is completed successfully
 */
public class TaskCompletedEvent extends DomainEvent {

    private final String taskId;
    private final TaskType type;
    private final String completedBy;
    private final String warehouseId;
    private final String referenceId;
    private final Duration actualDuration;
    private final boolean completedOnTime;

    public TaskCompletedEvent(
            String taskId,
            TaskType type,
            String completedBy,
            String warehouseId,
            String referenceId,
            Duration actualDuration,
            boolean completedOnTime
    ) {
        super();
        this.taskId = taskId;
        this.type = type;
        this.completedBy = completedBy;
        this.warehouseId = warehouseId;
        this.referenceId = referenceId;
        this.actualDuration = actualDuration;
        this.completedOnTime = completedOnTime;
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
}
