package com.paklog.wes.task.domain.event;

import com.paklog.domain.shared.DomainEvent;
import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.task.domain.valueobject.TaskType;

/**
 * Event published when a task is created
 */
public class TaskCreatedEvent extends DomainEvent {

    private final String taskId;
    private final TaskType type;
    private final Priority priority;
    private final String warehouseId;
    private final String zone;
    private final String referenceId;

    public TaskCreatedEvent(
            String taskId,
            TaskType type,
            Priority priority,
            String warehouseId,
            String zone,
            String referenceId
    ) {
        super();
        this.taskId = taskId;
        this.type = type;
        this.priority = priority;
        this.warehouseId = warehouseId;
        this.zone = zone;
        this.referenceId = referenceId;
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskType getType() {
        return type;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getZone() {
        return zone;
    }

    public String getReferenceId() {
        return referenceId;
    }
}
