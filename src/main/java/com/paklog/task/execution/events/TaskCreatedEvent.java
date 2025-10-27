package com.paklog.task.execution.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when a new task is created
 * CloudEvent Type: com.paklog.wes.task-execution.task.task.created.v1
 */
public record TaskCreatedEvent(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("task_type") String taskType,
    @JsonProperty("priority") String priority,
    @JsonProperty("zone_id") String zoneId,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("estimated_duration_seconds") int estimatedDurationSeconds
) {
    public static final String EVENT_TYPE = "com.paklog.wes.task-execution.task.task.created.v1";
}
