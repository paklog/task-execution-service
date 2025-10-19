package com.paklog.wes.task.domain.aggregate;

import com.paklog.domain.annotation.AggregateRoot;
import com.paklog.domain.shared.DomainEvent;
import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.task.domain.entity.TaskContext;
import com.paklog.wes.task.domain.event.TaskAssignedEvent;
import com.paklog.wes.task.domain.event.TaskCompletedEvent;
import com.paklog.wes.task.domain.event.TaskCreatedEvent;
import com.paklog.wes.task.domain.event.TaskFailedEvent;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskStatus;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Transient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * WorkTask aggregate root
 * Represents a unified task model for all warehouse work types
 */
@AggregateRoot
@Document(collection = "work_tasks")
public class WorkTask {

    @Id
    private String taskId;

    @Version
    private Long version;

    private TaskType type;
    private TaskStatus status;
    private Priority priority;
    private String assignedTo;
    private String warehouseId;
    private String zone;
    private Location taskLocation;
    private LocalDateTime createdAt;
    private LocalDateTime queuedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Duration estimatedDuration;
    private Duration actualDuration;
    private String referenceId;  // Reference to wave, order, receipt, etc.
    private LocalDateTime deadline;
    private TaskContext context;
    private String failureReason;
    private String cancellationReason;

    @Transient
    private List<DomainEvent> domainEvents = new ArrayList<>();

    /**
     * Create a new task
     */
    public static WorkTask create(
            TaskType type,
            String warehouseId,
            String zone,
            Location location,
            Priority priority,
            String referenceId,
            Duration estimatedDuration,
            LocalDateTime deadline,
            TaskContext context
    ) {
        WorkTask task = new WorkTask();
        task.taskId = generateTaskId();
        task.type = Objects.requireNonNull(type, "Task type cannot be null");
        task.warehouseId = Objects.requireNonNull(warehouseId, "Warehouse ID cannot be null");
        task.zone = zone;
        task.taskLocation = location;
        task.priority = priority != null ? priority : Priority.NORMAL;
        task.referenceId = Objects.requireNonNull(referenceId, "Reference ID cannot be null");
        task.estimatedDuration = estimatedDuration;
        task.deadline = deadline;
        task.context = Objects.requireNonNull(context, "Task context cannot be null");
        task.status = TaskStatus.PENDING;
        task.createdAt = LocalDateTime.now();

        // Validate context
        context.validate();

        // Register creation event
        task.registerEvent(new TaskCreatedEvent(
                task.taskId,
                task.type,
                task.priority,
                task.warehouseId,
                task.zone,
                task.referenceId
        ));

        return task;
    }

    /**
     * Queue the task for assignment
     */
    public void queue() {
        ensureStatus(TaskStatus.PENDING);
        this.status = TaskStatus.QUEUED;
        this.queuedAt = LocalDateTime.now();
    }

    /**
     * Assign task to a worker
     */
    public void assign(String workerId) {
        Objects.requireNonNull(workerId, "Worker ID cannot be null");
        ensureStatus(TaskStatus.QUEUED);

        this.assignedTo = workerId;
        this.status = TaskStatus.ASSIGNED;
        this.assignedAt = LocalDateTime.now();

        // Register assignment event
        registerEvent(new TaskAssignedEvent(
                this.taskId,
                this.type,
                this.assignedTo,
                this.warehouseId,
                this.zone
        ));
    }

    /**
     * Worker accepts the task
     */
    public void accept() {
        ensureStatus(TaskStatus.ASSIGNED);
        ensureAssigned();

        this.status = TaskStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }

    /**
     * Reject assignment and return to queue
     */
    public void reject(String reason) {
        ensureStatus(TaskStatus.ASSIGNED);

        this.assignedTo = null;
        this.assignedAt = null;
        this.status = TaskStatus.QUEUED;
    }

    /**
     * Start task execution
     */
    public void start() {
        ensureStatus(TaskStatus.ACCEPTED);
        ensureAssigned();

        this.status = TaskStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * Complete the task successfully
     */
    public void complete() {
        ensureStatus(TaskStatus.IN_PROGRESS);
        ensureAssigned();

        this.status = TaskStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.actualDuration = Duration.between(startedAt, completedAt);

        // Register completion event
        registerEvent(new TaskCompletedEvent(
                this.taskId,
                this.type,
                this.assignedTo,
                this.warehouseId,
                this.referenceId,
                this.actualDuration,
                isCompletedOnTime()
        ));
    }

    /**
     * Mark task as failed
     */
    public void fail(String reason) {
        Objects.requireNonNull(reason, "Failure reason cannot be null");
        ensureStatus(TaskStatus.IN_PROGRESS);

        this.status = TaskStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = LocalDateTime.now();

        if (startedAt != null) {
            this.actualDuration = Duration.between(startedAt, completedAt);
        }

        // Register failure event
        registerEvent(new TaskFailedEvent(
                this.taskId,
                this.type,
                this.assignedTo,
                this.warehouseId,
                this.referenceId,
                this.failureReason
        ));
    }

    /**
     * Cancel the task
     */
    public void cancel(String reason) {
        Objects.requireNonNull(reason, "Cancellation reason cannot be null");

        if (status.isTerminal()) {
            throw new IllegalStateException("Cannot cancel task in terminal status: " + status);
        }

        this.status = TaskStatus.CANCELLED;
        this.cancellationReason = reason;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Check if task is overdue
     */
    public boolean isOverdue() {
        if (deadline == null || status.isTerminal()) {
            return false;
        }
        return LocalDateTime.now().isAfter(deadline);
    }

    /**
     * Get priority score for queue ordering
     * Lower score = higher priority
     */
    public double getPriorityScore() {
        double base = priority.getValue() * 1000.0;

        // Age increases priority (reduces score)
        long ageMinutes = Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        base -= ageMinutes;

        // Overdue tasks get highest priority
        if (isOverdue()) {
            base -= 10000;
        }

        // Complexity affects priority
        if (context != null) {
            base += (context.getComplexityScore() * 100);
        }

        return base;
    }

    /**
     * Calculate performance metrics
     */
    public double getPerformanceRatio() {
        if (actualDuration == null || estimatedDuration == null) {
            return 1.0;
        }
        return (double) actualDuration.toSeconds() / estimatedDuration.toSeconds();
    }

    /**
     * Check if task was completed on time
     */
    public boolean isCompletedOnTime() {
        if (!status.equals(TaskStatus.COMPLETED) || deadline == null) {
            return false;
        }
        return completedAt.isBefore(deadline) || completedAt.equals(deadline);
    }

    // Validation helpers
    private void ensureStatus(TaskStatus expectedStatus) {
        if (this.status != expectedStatus) {
            throw new IllegalStateException(
                    String.format("Task must be in %s status but is %s", expectedStatus, this.status)
            );
        }
    }

    private void ensureAssigned() {
        if (assignedTo == null || assignedTo.isBlank()) {
            throw new IllegalStateException("Task must be assigned to a worker");
        }
    }

    // Domain event management
    protected void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() {
        return new ArrayList<>(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    // Utility methods
    private static String generateTaskId() {
        return "TASK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Getters and setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Long getVersion() {
        return version;
    }

    public TaskType getType() {
        return type;
    }

    public void setType(TaskType type) {
        this.type = type;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public Location getTaskLocation() {
        return taskLocation;
    }

    public void setTaskLocation(Location taskLocation) {
        this.taskLocation = taskLocation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getQueuedAt() {
        return queuedAt;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public Duration getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(Duration estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public Duration getActualDuration() {
        return actualDuration;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public TaskContext getContext() {
        return context;
    }

    public void setContext(TaskContext context) {
        this.context = context;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }
}
