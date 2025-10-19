package com.paklog.wes.task.domain.repository;

import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.valueobject.TaskStatus;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for WorkTask aggregate
 */
@Repository
public interface WorkTaskRepository extends MongoRepository<WorkTask, String> {

    /**
     * Find all tasks assigned to a specific worker
     */
    List<WorkTask> findByAssignedTo(String workerId);

    /**
     * Find all tasks by status
     */
    List<WorkTask> findByStatus(TaskStatus status);

    /**
     * Find all tasks by type and status
     */
    List<WorkTask> findByTypeAndStatus(TaskType type, TaskStatus status);

    /**
     * Find all tasks in a warehouse by status
     */
    List<WorkTask> findByWarehouseIdAndStatus(String warehouseId, TaskStatus status);

    /**
     * Find all tasks in a zone by status
     */
    List<WorkTask> findByZoneAndStatus(String zone, TaskStatus status);

    /**
     * Find all active tasks (assigned, accepted, or in progress)
     */
    @Query("{'status': {$in: ['ASSIGNED', 'ACCEPTED', 'IN_PROGRESS']}}")
    List<WorkTask> findActiveTasks();

    /**
     * Find all active tasks assigned to a worker
     */
    @Query("{'assignedTo': ?0, 'status': {$in: ['ASSIGNED', 'ACCEPTED', 'IN_PROGRESS']}}")
    List<WorkTask> findActiveTasksByWorker(String workerId);

    /**
     * Find all queued tasks by priority
     */
    @Query("{'status': 'QUEUED', 'warehouseId': ?0, 'zone': ?1}")
    List<WorkTask> findQueuedTasksByZone(String warehouseId, String zone);

    /**
     * Find overdue tasks
     */
    @Query("{'deadline': {$lt: ?0}, 'status': {$in: ['QUEUED', 'ASSIGNED', 'ACCEPTED', 'IN_PROGRESS']}}")
    List<WorkTask> findOverdueTasks(LocalDateTime now);

    /**
     * Find tasks by reference ID (wave, order, receipt, etc.)
     */
    List<WorkTask> findByReferenceId(String referenceId);

    /**
     * Find tasks by reference ID and status
     */
    List<WorkTask> findByReferenceIdAndStatus(String referenceId, TaskStatus status);

    /**
     * Find tasks created in a time range
     */
    @Query("{'createdAt': {$gte: ?0, $lte: ?1}}")
    List<WorkTask> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find completed tasks in a time range
     */
    @Query("{'status': 'COMPLETED', 'completedAt': {$gte: ?0, $lte: ?1}}")
    List<WorkTask> findCompletedInRange(LocalDateTime start, LocalDateTime end);

    /**
     * Find tasks by worker and completion time range
     */
    @Query("{'assignedTo': ?0, 'status': 'COMPLETED', 'completedAt': {$gte: ?1, $lte: ?2}}")
    List<WorkTask> findCompletedByWorkerInRange(String workerId, LocalDateTime start, LocalDateTime end);

    /**
     * Count tasks by status in a warehouse
     */
    @Query(value = "{'warehouseId': ?0, 'status': ?1}", count = true)
    long countByWarehouseIdAndStatus(String warehouseId, TaskStatus status);

    /**
     * Count active tasks for a worker
     */
    @Query(value = "{'assignedTo': ?0, 'status': {$in: ['ASSIGNED', 'ACCEPTED', 'IN_PROGRESS']}}", count = true)
    long countActiveTasksByWorker(String workerId);
}
