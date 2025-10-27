package com.paklog.task.execution.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when worker starts executing task
 * CloudEvent Type: com.paklog.wes.task-execution.task.task.started.v1
 */
public record TaskStartedEvent(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("started_at") Instant startedAt,
    @JsonProperty("estimated_completion_time") Instant estimatedCompletionTime
) {
    public static final String EVENT_TYPE = "com.paklog.wes.task-execution.task.task.started.v1";
}
