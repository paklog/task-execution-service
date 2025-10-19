package com.paklog.wes.task.infrastructure.assignment;

import com.paklog.wes.task.application.service.TaskManagementService;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.valueobject.TaskType;
import com.paklog.wes.task.infrastructure.queue.TaskQueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Intelligent task assignment engine
 * Assigns tasks to workers based on multiple scoring factors
 */
@Service
public class TaskAssignmentEngine {

    private static final Logger logger = LoggerFactory.getLogger(TaskAssignmentEngine.class);

    private final TaskQueueManager queueManager;
    private final TaskManagementService taskService;

    public TaskAssignmentEngine(TaskQueueManager queueManager, TaskManagementService taskService) {
        this.queueManager = queueManager;
        this.taskService = taskService;
    }

    /**
     * Get next task for a worker
     * Dequeues from eligible queues and assigns to worker
     */
    public Optional<WorkTask> getNextTaskForWorker(Worker worker) {
        logger.debug("Finding next task for worker {}", worker.workerId());

        // Dequeue from Redis
        Optional<String> taskId = queueManager.dequeue(
                worker.workerId(),
                worker.warehouseId(),
                worker.currentZone(),
                worker.capabilities()
        );

        if (taskId.isEmpty()) {
            logger.debug("No tasks available for worker {}", worker.workerId());
            return Optional.empty();
        }

        try {
            // Assign the task
            WorkTask task = taskService.assignTask(taskId.get(), worker.workerId());
            logger.info("Assigned task {} to worker {}", task.getTaskId(), worker.workerId());
            return Optional.of(task);

        } catch (Exception e) {
            logger.error("Failed to assign task {} to worker {}", taskId.get(), worker.workerId(), e);

            // Re-enqueue the task if assignment failed
            try {
                WorkTask task = taskService.findTaskById(taskId.get());
                queueManager.enqueue(task);
            } catch (Exception ex) {
                logger.error("Failed to re-enqueue task {}", taskId.get(), ex);
            }

            return Optional.empty();
        }
    }

    /**
     * Assign task to best available worker
     * Uses worker scoring algorithm
     */
    public AssignmentResult assignTaskToBestWorker(WorkTask task, List<Worker> availableWorkers) {
        logger.debug("Finding best worker for task {}", task.getTaskId());

        // Filter workers who can perform this task type
        List<Worker> eligibleWorkers = availableWorkers.stream()
                .filter(worker -> worker.canPerform(task.getType()))
                .filter(worker -> worker.warehouseId().equals(task.getWarehouseId()))
                .collect(Collectors.toList());

        if (eligibleWorkers.isEmpty()) {
            logger.warn("No eligible workers found for task {}", task.getTaskId());
            return AssignmentResult.noEligibleWorker(task.getTaskId());
        }

        // Score each worker
        List<WorkerScore> scores = eligibleWorkers.stream()
                .map(worker -> calculateScore(worker, task))
                .sorted(Comparator.reverseOrder()) // Highest score first
                .collect(Collectors.toList());

        logger.debug("Worker scores for task {}: {}", task.getTaskId(), scores);

        // Try assigning to best workers in order
        for (WorkerScore score : scores) {
            try {
                taskService.assignTask(task.getTaskId(), score.worker().workerId());
                logger.info("Assigned task {} to worker {} (score: {})",
                        task.getTaskId(), score.worker().workerId(), score.score());
                return AssignmentResult.success(task.getTaskId(), score.worker().workerId(), score.score());

            } catch (Exception e) {
                logger.warn("Failed to assign task {} to worker {}: {}",
                        task.getTaskId(), score.worker().workerId(), e.getMessage());
            }
        }

        return AssignmentResult.failed(task.getTaskId(), "All assignment attempts failed");
    }

    /**
     * Calculate worker score for a task
     * Higher score = better match
     */
    private WorkerScore calculateScore(Worker worker, WorkTask task) {
        double score = 100.0;

        // Distance score (0-30 points)
        // Workers in the same zone get higher scores
        if (task.getZone() != null && task.getZone().equals(worker.currentZone())) {
            score += 30.0;

            // If task has location and worker has location, calculate distance
            if (task.getTaskLocation() != null && worker.currentLocation() != null) {
                double distance = task.getTaskLocation().distanceFrom(worker.currentLocation());
                // Closer is better (inverse scoring)
                score += Math.max(0, 20 - (distance / 10.0));
            }
        } else {
            // Penalty for different zone
            score -= 20.0;
        }

        // Current workload (0-20 points)
        // Workers with fewer active tasks get higher scores
        int activeTasks = worker.activeTaskCount();
        score += Math.max(0, 20 - (activeTasks * 5));

        // Task type specialization (0-20 points)
        if (worker.hasSpecialization(task.getType())) {
            score += 20.0;
        }

        // Priority match (0-10 points)
        // Workers with less priority work get assigned higher priority tasks
        if (task.getPriority().getValue() <= 2) { // HIGH or CRITICAL
            score += 10.0;
        }

        // Performance bonus (0-10 points)
        // Based on historical performance (placeholder - would be calculated from metrics)
        score += worker.performanceRating() * 10.0; // 0.0 to 1.0

        return new WorkerScore(worker, score);
    }

    /**
     * Batch assign tasks to multiple workers
     */
    public List<AssignmentResult> batchAssign(List<WorkTask> tasks, List<Worker> workers) {
        List<AssignmentResult> results = new ArrayList<>();

        for (WorkTask task : tasks) {
            AssignmentResult result = assignTaskToBestWorker(task, workers);
            results.add(result);

            if (result.success()) {
                // Remove assigned worker from available pool
                workers.removeIf(w -> w.workerId().equals(result.workerId()));
            }
        }

        return results;
    }

    /**
     * Get task recommendations for a worker
     * Returns tasks the worker could do, sorted by score
     */
    public List<TaskRecommendation> getTaskRecommendations(Worker worker, List<WorkTask> availableTasks) {
        return availableTasks.stream()
                .filter(task -> worker.canPerform(task.getType()))
                .filter(task -> worker.warehouseId().equals(task.getWarehouseId()))
                .map(task -> {
                    WorkerScore score = calculateScore(worker, task);
                    return new TaskRecommendation(
                            task.getTaskId(),
                            task.getType(),
                            task.getPriority(),
                            task.getZone(),
                            score.score(),
                            calculateEstimatedWalkTime(worker, task)
                    );
                })
                .sorted(Comparator.comparing(TaskRecommendation::score).reversed())
                .collect(Collectors.toList());
    }

    private int calculateEstimatedWalkTime(Worker worker, WorkTask task) {
        if (task.getTaskLocation() == null || worker.currentLocation() == null) {
            return 0;
        }

        // Simplified calculation: ~6 seconds per distance unit
        double distance = task.getTaskLocation().distanceFrom(worker.currentLocation());
        return (int) (distance * 6);
    }
}
