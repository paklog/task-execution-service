package com.paklog.wes.task.infrastructure.assignment;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.task.domain.valueobject.TaskType;

/**
 * Task recommendation for a worker
 */
public record TaskRecommendation(
        String taskId,
        TaskType type,
        Priority priority,
        String zone,
        double score,
        int estimatedWalkTimeSeconds
) {
    public boolean isHighScore() {
        return score > 120.0; // Excellent match
    }

    public boolean isMediumScore() {
        return score > 80.0 && score <= 120.0; // Good match
    }

    public boolean isLowScore() {
        return score <= 80.0; // Poor match
    }
}
