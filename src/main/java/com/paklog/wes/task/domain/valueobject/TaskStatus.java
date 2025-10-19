package com.paklog.wes.task.domain.valueobject;

import java.util.Set;

/**
 * Task lifecycle status
 */
public enum TaskStatus {
    /**
     * Task created but not yet queued
     */
    PENDING,

    /**
     * Task in queue waiting for assignment
     */
    QUEUED,

    /**
     * Task assigned to a worker but not yet accepted
     */
    ASSIGNED,

    /**
     * Worker accepted the task
     */
    ACCEPTED,

    /**
     * Task execution in progress
     */
    IN_PROGRESS,

    /**
     * Task completed successfully
     */
    COMPLETED,

    /**
     * Task cancelled
     */
    CANCELLED,

    /**
     * Task failed with exception
     */
    FAILED;

    /**
     * Check if this status allows transition to new status
     */
    public boolean canTransitionTo(TaskStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == QUEUED || newStatus == CANCELLED;
            case QUEUED -> newStatus == ASSIGNED || newStatus == CANCELLED;
            case ASSIGNED -> newStatus == ACCEPTED || newStatus == QUEUED || newStatus == CANCELLED;
            case ACCEPTED -> newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case IN_PROGRESS -> newStatus == COMPLETED || newStatus == FAILED || newStatus == CANCELLED;
            case COMPLETED, CANCELLED, FAILED -> false; // Terminal states
        };
    }

    /**
     * Check if this is a terminal status
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == FAILED;
    }

    /**
     * Check if this is an active status (task can still be worked on)
     */
    public boolean isActive() {
        return this == ASSIGNED || this == ACCEPTED || this == IN_PROGRESS;
    }

    /**
     * Get all valid next statuses from current status
     */
    public Set<TaskStatus> getValidTransitions() {
        return Set.of(values()).stream()
                .filter(this::canTransitionTo)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Validate transition and throw exception if invalid
     */
    public void ensureCanTransitionTo(TaskStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s", this, newStatus)
            );
        }
    }
}
