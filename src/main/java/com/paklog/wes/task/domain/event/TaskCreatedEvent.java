package com.paklog.wes.task.domain.event;

import com.paklog.task.execution.domain.shared.DomainEvent;
import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.wes.task.domain.valueobject.TaskType;

import java.time.Instant;

/**
 * Event published when a task is created
 */
public class TaskCreatedEvent implements DomainEvent {

    private final String taskId;
    private final TaskType type;
    private final Priority priority;
    private final String warehouseId;
    private final String zone;
    private final String referenceId;
    private final Instant occurredOn;

    public TaskCreatedEvent(
            String taskId,
            TaskType type,
            Priority priority,
            String warehouseId,
            String zone,
            String referenceId
    ) {
        this.taskId = taskId;
        this.type = type;
        this.priority = priority;
        this.warehouseId = warehouseId;
        this.zone = zone;
        this.referenceId = referenceId;
        this.occurredOn = Instant.now();
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

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }

    @Override
    public String eventType() {
        return "TaskCreated";
    }
}
