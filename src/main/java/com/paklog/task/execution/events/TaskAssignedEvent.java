package com.paklog.task.execution.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when task is assigned to a worker
 * CloudEvent Type: com.paklog.wes.task-execution.task.task.assigned.v1
 */
public record TaskAssignedEvent(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("assigned_at") Instant assignedAt,
    @JsonProperty("priority") String priority,
    @JsonProperty("zone_id") String zoneId
) {
    public static final String EVENT_TYPE = "com.paklog.wes.task-execution.task.task.assigned.v1";
}
