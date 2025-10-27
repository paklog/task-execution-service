package com.paklog.wes.task.domain.event;

import com.paklog.task.execution.domain.shared.DomainEvent;
import com.paklog.wes.task.domain.valueobject.TaskType;

import java.time.Instant;

/**
 * Event published when a task is assigned to a worker
 */
public class TaskAssignedEvent implements DomainEvent {

    private final String taskId;
    private final TaskType type;
    private final String assignedTo;
    private final String warehouseId;
    private final String zone;
    private final Instant occurredOn;

    public TaskAssignedEvent(
            String taskId,
            TaskType type,
            String assignedTo,
            String warehouseId,
            String zone
    ) {
        this.taskId = taskId;
        this.type = type;
        this.assignedTo = assignedTo;
        this.warehouseId = warehouseId;
        this.zone = zone;
        this.occurredOn = Instant.now();
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskType getType() {
        return type;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getZone() {
        return zone;
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }

    @Override
    public String eventType() {
        return "TaskAssigned";
    }
}
