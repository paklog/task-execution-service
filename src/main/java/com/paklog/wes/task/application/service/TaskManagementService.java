package com.paklog.wes.task.application.service;

import com.paklog.wes.task.application.command.CreateTaskCommand;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.repository.WorkTaskRepository;
import com.paklog.wes.task.domain.valueobject.TaskStatus;
import com.paklog.wes.task.domain.valueobject.TaskType;
import com.paklog.wes.task.infrastructure.queue.TaskQueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Application service for task management
 */
@Service
public class TaskManagementService {

    private static final Logger logger = LoggerFactory.getLogger(TaskManagementService.class);

    private final WorkTaskRepository taskRepository;
    private final TaskQueueManager queueManager;

    public TaskManagementService(WorkTaskRepository taskRepository, TaskQueueManager queueManager) {
        this.taskRepository = taskRepository;
        this.queueManager = queueManager;
    }

    /**
     * Create a new task
     */
    @Transactional
    public WorkTask createTask(CreateTaskCommand command) {
        logger.info("Creating task: type={}, warehouseId={}, referenceId={}",
                command.type(), command.warehouseId(), command.referenceId());

        WorkTask task = WorkTask.create(
                command.type(),
                command.warehouseId(),
                command.zone(),
                command.location(),
                command.priority(),
                command.referenceId(),
                command.estimatedDuration(),
                command.deadline(),
                command.context()
        );

        // Queue the task immediately after creation
        task.queue();

        WorkTask savedTask = taskRepository.save(task);

        // Add to Redis queue for assignment
        queueManager.enqueue(savedTask);

        logger.info("Task created and queued: taskId={}", savedTask.getTaskId());
        return savedTask;
    }

    /**
     * Assign task to a worker
     */
    @Transactional
    public WorkTask assignTask(String taskId, String workerId) {
        logger.info("Assigning task {} to worker {}", taskId, workerId);

        WorkTask task = findTaskById(taskId);
        task.assign(workerId);

        WorkTask savedTask = taskRepository.save(task);

        // Remove from queue since it's now assigned
        queueManager.remove(savedTask);

        logger.info("Task assigned successfully: taskId={}, workerId={}", taskId, workerId);

        return savedTask;
    }

    /**
     * Worker accepts assigned task
     */
    @Transactional
    public WorkTask acceptTask(String taskId) {
        logger.info("Accepting task {}", taskId);

        WorkTask task = findTaskById(taskId);
        task.accept();

        WorkTask savedTask = taskRepository.save(task);
        logger.info("Task accepted: taskId={}", taskId);

        return savedTask;
    }

    /**
     * Worker rejects assigned task
     */
    @Transactional
    public WorkTask rejectTask(String taskId, String reason) {
        logger.info("Rejecting task {}: reason={}", taskId, reason);

        WorkTask task = findTaskById(taskId);
        task.reject(reason);

        WorkTask savedTask = taskRepository.save(task);

        // Re-enqueue the task for another worker
        queueManager.enqueue(savedTask);

        logger.info("Task rejected and returned to queue: taskId={}", taskId);

        return savedTask;
    }

    /**
     * Start task execution
     */
    @Transactional
    public WorkTask startTask(String taskId) {
        logger.info("Starting task {}", taskId);

        WorkTask task = findTaskById(taskId);
        task.start();

        WorkTask savedTask = taskRepository.save(task);
        logger.info("Task started: taskId={}", taskId);

        return savedTask;
    }

    /**
     * Complete task
     */
    @Transactional
    public WorkTask completeTask(String taskId) {
        logger.info("Completing task {}", taskId);

        WorkTask task = findTaskById(taskId);
        task.complete();

        WorkTask savedTask = taskRepository.save(task);
        logger.info("Task completed: taskId={}, duration={}", taskId, task.getActualDuration());

        return savedTask;
    }

    /**
     * Mark task as failed
     */
    @Transactional
    public WorkTask failTask(String taskId, String reason) {
        logger.info("Failing task {}: reason={}", taskId, reason);

        WorkTask task = findTaskById(taskId);
        task.fail(reason);

        WorkTask savedTask = taskRepository.save(task);
        logger.info("Task failed: taskId={}", taskId);

        return savedTask;
    }

    /**
     * Cancel task
     */
    @Transactional
    public WorkTask cancelTask(String taskId, String reason) {
        logger.info("Cancelling task {}: reason={}", taskId, reason);

        WorkTask task = findTaskById(taskId);
        task.cancel(reason);

        WorkTask savedTask = taskRepository.save(task);

        // Remove from queue if it was queued
        queueManager.remove(savedTask);

        logger.info("Task cancelled: taskId={}", taskId);

        return savedTask;
    }

    // Query methods
    public WorkTask findTaskById(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found: " + taskId));
    }

    public List<WorkTask> findTasksByWorker(String workerId) {
        return taskRepository.findByAssignedTo(workerId);
    }

    public List<WorkTask> findActiveTasksByWorker(String workerId) {
        return taskRepository.findActiveTasksByWorker(workerId);
    }

    public List<WorkTask> findTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    public List<WorkTask> findTasksByTypeAndStatus(TaskType type, TaskStatus status) {
        return taskRepository.findByTypeAndStatus(type, status);
    }

    public List<WorkTask> findTasksByWarehouseAndStatus(String warehouseId, TaskStatus status) {
        return taskRepository.findByWarehouseIdAndStatus(warehouseId, status);
    }

    public List<WorkTask> findTasksByZoneAndStatus(String zone, TaskStatus status) {
        return taskRepository.findByZoneAndStatus(zone, status);
    }

    public List<WorkTask> findQueuedTasksByZone(String warehouseId, String zone) {
        return taskRepository.findQueuedTasksByZone(warehouseId, zone);
    }

    public List<WorkTask> findOverdueTasks() {
        return taskRepository.findOverdueTasks(LocalDateTime.now());
    }

    public List<WorkTask> findTasksByReference(String referenceId) {
        return taskRepository.findByReferenceId(referenceId);
    }

    public long countActiveTasksByWorker(String workerId) {
        return taskRepository.countActiveTasksByWorker(workerId);
    }

    /**
     * Custom exception for task not found
     */
    public static class TaskNotFoundException extends RuntimeException {
        public TaskNotFoundException(String message) {
            super(message);
        

}
}
}
