package com.paklog.task.execution.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when task is successfully completed
 * CloudEvent Type: com.paklog.wes.task-execution.task.task.completed.v1
 */
public record TaskCompletedEvent(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("completed_at") Instant completedAt,
    @JsonProperty("duration_seconds") long durationSeconds,
    @JsonProperty("items_processed") int itemsProcessed
) {
    public static final String EVENT_TYPE = "com.paklog.wes.task-execution.task.task.completed.v1";
}
