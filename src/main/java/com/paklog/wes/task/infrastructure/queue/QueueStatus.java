package com.paklog.wes.task.infrastructure.queue;

import com.paklog.wes.task.domain.valueobject.TaskType;

/**
 * Queue status information
 */
public record QueueStatus(
        String queueKey,
        String warehouseId,
        String zone,
        TaskType type,
        int depth,
        String oldestTaskId
) {
    public boolean isEmpty() {
        return depth == 0;
    }

    public boolean hasBacklog() {
        return depth > 10; // Configurable threshold
    }
}
