package com.paklog.wes.task.infrastructure.assignment;

/**
 * Result of a task assignment attempt
 */
public record AssignmentResult(
        String taskId,
        boolean success,
        String workerId,
        Double score,
        String errorMessage
) {
    public static AssignmentResult success(String taskId, String workerId, double score) {
        return new AssignmentResult(taskId, true, workerId, score, null);
    }

    public static AssignmentResult failed(String taskId, String errorMessage) {
        return new AssignmentResult(taskId, false, null, null, errorMessage);
    }

    public static AssignmentResult noEligibleWorker(String taskId) {
        return new AssignmentResult(taskId, false, null, null, "No eligible workers available");
    }
}
