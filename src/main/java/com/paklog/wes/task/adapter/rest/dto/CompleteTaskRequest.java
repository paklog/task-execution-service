package com.paklog.wes.task.adapter.rest.dto;

import java.util.Map;

/**
 * Request to complete a task
 */
public record CompleteTaskRequest(
        Map<String, Object> metadata
) {
}
