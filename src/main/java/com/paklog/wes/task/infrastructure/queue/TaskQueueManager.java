package com.paklog.wes.task.infrastructure.queue;

import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Task queue management using Redis sorted sets
 * Tasks are queued by priority score (lower score = higher priority)
 */
@Service
public class TaskQueueManager {

    private static final Logger logger = LoggerFactory.getLogger(TaskQueueManager.class);
    private static final String QUEUE_PREFIX = "task:queue:";

    private final RedisTemplate<String, String> redisTemplate;

    public TaskQueueManager(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Enqueue a task for assignment
     * Tasks are scored by priority, with lower scores getting higher priority
     */
    public void enqueue(WorkTask task) {
        String queueKey = buildQueueKey(task.getWarehouseId(), task.getZone(), task.getType());
        double score = task.getPriorityScore();

        logger.debug("Enqueuing task {} to queue {} with score {}",
                task.getTaskId(), queueKey, score);

        redisTemplate.opsForZSet().add(queueKey, task.getTaskId(), score);

        logger.info("Task {} enqueued to {}", task.getTaskId(), queueKey);
    }

    /**
     * Dequeue highest priority task from eligible queues
     * Returns task ID if found, empty if no tasks available
     */
    public Optional<String> dequeue(String workerId, String warehouseId, String zone, Set<TaskType> capabilities) {
        List<String> eligibleQueues = getEligibleQueues(warehouseId, zone, capabilities);

        logger.debug("Worker {} checking queues: {}", workerId, eligibleQueues);

        for (String queueKey : eligibleQueues) {
            // Get highest priority task (lowest score)
            Set<String> taskIds = redisTemplate.opsForZSet().range(queueKey, 0, 0);

            if (taskIds != null && !taskIds.isEmpty()) {
                String taskId = taskIds.iterator().next();

                // Remove from queue atomically
                Long removed = redisTemplate.opsForZSet().remove(queueKey, taskId);

                if (removed != null && removed > 0) {
                    logger.info("Dequeued task {} from queue {} for worker {}",
                            taskId, queueKey, workerId);
                    return Optional.of(taskId);
                }
            }
        }

        logger.debug("No tasks available for worker {} in queues: {}", workerId, eligibleQueues);
        return Optional.empty();
    }

    /**
     * Remove a task from its queue
     */
    public void remove(WorkTask task) {
        String queueKey = buildQueueKey(task.getWarehouseId(), task.getZone(), task.getType());
        redisTemplate.opsForZSet().remove(queueKey, task.getTaskId());
        logger.debug("Removed task {} from queue {}", task.getTaskId(), queueKey);
    }

    /**
     * Get queue status for a specific queue
     */
    public QueueStatus getQueueStatus(String warehouseId, String zone, TaskType type) {
        String queueKey = buildQueueKey(warehouseId, zone, type);
        Long size = redisTemplate.opsForZSet().size(queueKey);

        // Get oldest task (first in queue by time added, but we use score)
        Set<String> oldestTasks = redisTemplate.opsForZSet().range(queueKey, 0, 0);
        String oldestTaskId = oldestTasks != null && !oldestTasks.isEmpty()
                ? oldestTasks.iterator().next()
                : null;

        return new QueueStatus(
                queueKey,
                warehouseId,
                zone,
                type,
                size != null ? size.intValue() : 0,
                oldestTaskId
        );
    }

    /**
     * Get status of all queues for a warehouse
     */
    public List<QueueStatus> getAllQueueStatus(String warehouseId) {
        // Get all queue keys for this warehouse
        Set<String> keys = redisTemplate.keys(QUEUE_PREFIX + warehouseId + ":*");

        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        return keys.stream()
                .map(this::parseQueueStatus)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Get queue depth (number of tasks) for a specific queue
     */
    public int getQueueDepth(String warehouseId, String zone, TaskType type) {
        String queueKey = buildQueueKey(warehouseId, zone, type);
        Long size = redisTemplate.opsForZSet().size(queueKey);
        return size != null ? size.intValue() : 0;
    }

    /**
     * Peek at the next task without removing it
     */
    public Optional<String> peek(String warehouseId, String zone, TaskType type) {
        String queueKey = buildQueueKey(warehouseId, zone, type);
        Set<String> taskIds = redisTemplate.opsForZSet().range(queueKey, 0, 0);

        if (taskIds != null && !taskIds.isEmpty()) {
            return Optional.of(taskIds.iterator().next());
        }

        return Optional.empty();
    }

    /**
     * Clear all tasks from a queue
     */
    public void clearQueue(String warehouseId, String zone, TaskType type) {
        String queueKey = buildQueueKey(warehouseId, zone, type);
        redisTemplate.delete(queueKey);
        logger.info("Cleared queue {}", queueKey);
    }

    // Helper methods

    private String buildQueueKey(String warehouseId, String zone, TaskType type) {
        return QUEUE_PREFIX + warehouseId + ":" + zone + ":" + type.name();
    }

    private List<String> getEligibleQueues(String warehouseId, String zone, Set<TaskType> capabilities) {
        List<String> queues = new ArrayList<>();

        for (TaskType type : capabilities) {
            queues.add(buildQueueKey(warehouseId, zone, type));
        }

        return queues;
    }

    private Optional<QueueStatus> parseQueueStatus(String queueKey) {
        try {
            // Parse: task:queue:WH-001:ZONE-A:PICK
            String[] parts = queueKey.split(":");
            if (parts.length < 5) {
                return Optional.empty();
            }

            String warehouseId = parts[2];
            String zone = parts[3];
            TaskType type = TaskType.valueOf(parts[4]);

            Long size = redisTemplate.opsForZSet().size(queueKey);
            Set<String> oldestTasks = redisTemplate.opsForZSet().range(queueKey, 0, 0);
            String oldestTaskId = oldestTasks != null && !oldestTasks.isEmpty()
                    ? oldestTasks.iterator().next()
                    : null;

            return Optional.of(new QueueStatus(
                    queueKey,
                    warehouseId,
                    zone,
                    type,
                    size != null ? size.intValue() : 0,
                    oldestTaskId
            ));
        } catch (Exception e) {
            logger.warn("Failed to parse queue key: {}", queueKey, e);
            return Optional.empty();
        

}
}
}
