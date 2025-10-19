package com.paklog.wes.task.domain.aggregate;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.task.domain.entity.PickTaskContext;
import com.paklog.wes.task.domain.entity.TaskContext;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskStatus;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WorkTask aggregate
 */
class WorkTaskTest {

    private TaskContext taskContext;
    private Location location;

    @BeforeEach
    void setUp() {
        location = new Location("A", "01", "02", "03");

        // Create pick instructions
        var instructions = new ArrayList<PickTaskContext.PickInstruction>();
        instructions.add(new PickTaskContext.PickInstruction("SKU-001", 5, location, "LPN-001"));

        taskContext = new PickTaskContext(
                "WAVE-001",
                "ORDER-001",
                PickTaskContext.PickStrategy.DISCRETE,
                instructions
        );
    }

    @Test
    @DisplayName("Should create task successfully with valid data")
    void shouldCreateTaskSuccessfully() {
        // When
        WorkTask task = WorkTask.create(
                TaskType.PICK,
                "WH-001",
                "ZONE-A",
                location,
                Priority.HIGH,
                "WAVE-001",
                Duration.ofMinutes(10),
                LocalDateTime.now().plusHours(2),
                taskContext
        );

        // Then
        assertNotNull(task.getTaskId());
        assertEquals(TaskType.PICK, task.getType());
        assertEquals("WH-001", task.getWarehouseId());
        assertEquals("ZONE-A", task.getZone());
        assertEquals(Priority.HIGH, task.getPriority());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertNotNull(task.getCreatedAt());
        assertEquals(1, task.getDomainEvents().size());
    }

    @Test
    @DisplayName("Should use default priority when not specified")
    void shouldUseDefaultPriority() {
        // When
        WorkTask task = WorkTask.create(
                TaskType.PICK,
                "WH-001",
                "ZONE-A",
                location,
                null,
                "WAVE-001",
                Duration.ofMinutes(10),
                LocalDateTime.now().plusHours(2),
                taskContext
        );

        // Then
        assertEquals(Priority.NORMAL, task.getPriority());
    }

    @Test
    @DisplayName("Should queue task successfully from pending status")
    void shouldQueueTaskSuccessfully() {
        // Given
        WorkTask task = WorkTask.create(
                TaskType.PICK,
                "WH-001",
                "ZONE-A",
                location,
                Priority.HIGH,
                "WAVE-001",
                Duration.ofMinutes(10),
                LocalDateTime.now().plusHours(2),
                taskContext
        );

        // When
        task.queue();

        // Then
        assertEquals(TaskStatus.QUEUED, task.getStatus());
        assertNotNull(task.getQueuedAt());
    }

    @Test
    @DisplayName("Should assign task to worker successfully")
    void shouldAssignTaskSuccessfully() {
        // Given
        WorkTask task = WorkTask.create(
                TaskType.PICK,
                "WH-001",
                "ZONE-A",
                location,
                Priority.HIGH,
                "WAVE-001",
                Duration.ofMinutes(10),
                LocalDateTime.now().plusHours(2),
                taskContext
        );
        task.queue();
        task.clearDomainEvents();

        // When
        task.assign("WORKER-123");

        // Then
        assertEquals(TaskStatus.ASSIGNED, task.getStatus());
        assertEquals("WORKER-123", task.getAssignedTo());
        assertNotNull(task.getAssignedAt());
        assertEquals(1, task.getDomainEvents().size());
    }

    @Test
    @DisplayName("Should accept assigned task successfully")
    void shouldAcceptTaskSuccessfully() {
        // Given
        WorkTask task = createAssignedTask();

        // When
        task.accept();

        // Then
        assertEquals(TaskStatus.ACCEPTED, task.getStatus());
        assertNotNull(task.getAcceptedAt());
    }

    @Test
    @DisplayName("Should reject task and return to queue")
    void shouldRejectTaskSuccessfully() {
        // Given
        WorkTask task = createAssignedTask();

        // When
        task.reject("Worker too busy");

        // Then
        assertEquals(TaskStatus.QUEUED, task.getStatus());
        assertNull(task.getAssignedTo());
        assertNull(task.getAssignedAt());
    }

    @Test
    @DisplayName("Should start task execution from accepted status")
    void shouldStartTaskSuccessfully() {
        // Given
        WorkTask task = createAssignedTask();
        task.accept();

        // When
        task.start();

        // Then
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertNotNull(task.getStartedAt());
    }

    @Test
    @DisplayName("Should complete task successfully")
    void shouldCompleteTaskSuccessfully() {
        // Given
        WorkTask task = createInProgressTask();
        task.clearDomainEvents();

        // When
        task.complete();

        // Then
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        assertNotNull(task.getCompletedAt());
        assertNotNull(task.getActualDuration());
        assertEquals(1, task.getDomainEvents().size());
    }

    @Test
    @DisplayName("Should fail task with reason")
    void shouldFailTaskSuccessfully() {
        // Given
        WorkTask task = createInProgressTask();
        task.clearDomainEvents();

        // When
        task.fail("Equipment malfunction");

        // Then
        assertEquals(TaskStatus.FAILED, task.getStatus());
        assertEquals("Equipment malfunction", task.getFailureReason());
        assertNotNull(task.getCompletedAt());
        assertEquals(1, task.getDomainEvents().size());
    }

    @Test
    @DisplayName("Should cancel task from non-terminal status")
    void shouldCancelTaskSuccessfully() {
        // Given
        WorkTask task = createAssignedTask();

        // When
        task.cancel("Order cancelled");

        // Then
        assertEquals(TaskStatus.CANCELLED, task.getStatus());
        assertEquals("Order cancelled", task.getCancellationReason());
        assertNotNull(task.getCompletedAt());
    }

    @Test
    @DisplayName("Should throw exception when cancelling completed task")
    void shouldThrowExceptionWhenCancellingCompletedTask() {
        // Given
        WorkTask task = createInProgressTask();
        task.complete();

        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            task.cancel("Too late");
        });
    }

    @Test
    @DisplayName("Should throw exception when starting task from non-accepted status")
    void shouldThrowExceptionWhenStartingFromNonAcceptedStatus() {
        // Given
        WorkTask task = createAssignedTask();

        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            task.start();
        });
    }

    @Test
    @DisplayName("Should throw exception when completing non-in-progress task")
    void shouldThrowExceptionWhenCompletingNonInProgressTask() {
        // Given
        WorkTask task = createAssignedTask();
        task.accept();

        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            task.complete();
        });
    }

    @Test
    @DisplayName("Should throw exception when assigning task from non-queued status")
    void shouldThrowExceptionWhenAssigningFromNonQueuedStatus() {
        // Given
        WorkTask task = WorkTask.create(
                TaskType.PICK,
                "WH-001",
                "ZONE-A",
                location,
                Priority.HIGH,
                "WAVE-001",
                Duration.ofMinutes(10),
                LocalDateTime.now().plusHours(2),
                taskContext
        );

        // When/Then - task is PENDING, not QUEUED
        assertThrows(IllegalStateException.class, () -> {
            task.assign("WORKER-123");
        });
    }

    @Test
    @DisplayName("Should detect overdue tasks correctly")
    void shouldDetectOverdueTask() {
        // Given
        WorkTask task = WorkTask.create(
                TaskType.PICK,
                "WH-001",
                "ZONE-A",
                location,
                Priority.HIGH,
                "WAVE-001",
                Duration.ofMinutes(10),
                LocalDateTime.now().minusHours(1), // Deadline in past
                taskContext
        );

        // When/Then
        assertTrue(task.isOverdue());
    }

    @Test
    @DisplayName("Should calculate priority score correctly")
    void shouldCalculatePriorityScore() {
        // Given
        WorkTask task = WorkTask.create(
                TaskType.PICK,
                "WH-001",
                "ZONE-A",
                location,
                Priority.CRITICAL,
                "WAVE-001",
                Duration.ofMinutes(10),
                LocalDateTime.now().plusHours(2),
                taskContext
        );

        // When
        double score = task.getPriorityScore();

        // Then
        assertTrue(score > 0, "Priority score should be positive");
    }

    @Test
    @DisplayName("Should calculate performance ratio correctly")
    void shouldCalculatePerformanceRatio() {
        // Given
        WorkTask task = createInProgressTask();
        task.complete();

        // When
        double ratio = task.getPerformanceRatio();

        // Then
        assertTrue(ratio >= 0, "Performance ratio should be non-negative");
        assertNotNull(task.getActualDuration());
        assertNotNull(task.getEstimatedDuration());
    }

    @Test
    @DisplayName("Should determine if task completed on time")
    void shouldDetermineCompletedOnTime() {
        // Given
        WorkTask task = WorkTask.create(
                TaskType.PICK,
                "WH-001",
                "ZONE-A",
                location,
                Priority.HIGH,
                "WAVE-001",
                Duration.ofMinutes(10),
                LocalDateTime.now().plusHours(2),
                taskContext
        );
        task.queue();
        task.assign("WORKER-123");
        task.accept();
        task.start();
        task.complete();

        // When/Then
        assertTrue(task.isCompletedOnTime());
    }

    @Test
    @DisplayName("Should clear domain events")
    void shouldClearDomainEvents() {
        // Given
        WorkTask task = WorkTask.create(
                TaskType.PICK,
                "WH-001",
                "ZONE-A",
                location,
                Priority.HIGH,
                "WAVE-001",
                Duration.ofMinutes(10),
                LocalDateTime.now().plusHours(2),
                taskContext
        );
        assertEquals(1, task.getDomainEvents().size());

        // When
        task.clearDomainEvents();

        // Then
        assertEquals(0, task.getDomainEvents().size());
    }

    // Helper methods
    private WorkTask createAssignedTask() {
        WorkTask task = WorkTask.create(
                TaskType.PICK,
                "WH-001",
                "ZONE-A",
                location,
                Priority.HIGH,
                "WAVE-001",
                Duration.ofMinutes(10),
                LocalDateTime.now().plusHours(2),
                taskContext
        );
        task.queue();
        task.assign("WORKER-123");
        return task;
    }

    private WorkTask createInProgressTask() {
        WorkTask task = createAssignedTask();
        task.accept();
        task.start();
        return task;
    }
}
