package com.paklog.wes.task.infrastructure.assignment;

/**
 * Worker score for task assignment
 * Implements Comparable for sorting by score (higher is better)
 */
public record WorkerScore(
        Worker worker,
        double score
) implements Comparable<WorkerScore> {

    @Override
    public int compareTo(WorkerScore other) {
        return Double.compare(this.score, other.score);
    }

    @Override
    public String toString() {
        return String.format("WorkerScore{workerId=%s, score=%.2f}",
                worker.workerId(), score);
    }
}
