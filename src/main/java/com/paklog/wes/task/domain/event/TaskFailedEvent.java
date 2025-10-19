package com.paklog.wes.task.domain.event;

import com.paklog.domain.shared.DomainEvent;
import com.paklog.wes.task.domain.valueobject.TaskType;

/**
 * Event published when a task fails
 */
public class TaskFailedEvent extends DomainEvent {

    private final String taskId;
    private final TaskType type;
    private final String failedBy;
    private final String warehouseId;
    private final String referenceId;
    private final String failureReason;

    public TaskFailedEvent(
            String taskId,
            TaskType type,
            String failedBy,
            String warehouseId,
            String referenceId,
            String failureReason
    ) {
        super();
        this.taskId = taskId;
        this.type = type;
        this.failedBy = failedBy;
        this.warehouseId = warehouseId;
        this.referenceId = referenceId;
        this.failureReason = failureReason;
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
}
