package com.paklog.wes.task.infrastructure.assignment;

import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.wes.task.application.service.TaskManagementService;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.entity.PickTaskContext;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskType;
import com.paklog.wes.task.infrastructure.queue.TaskQueueManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskAssignmentEngineTest {

    @Mock
    private TaskQueueManager queueManager;

    @Mock
    private TaskManagementService taskService;

    @InjectMocks
    private TaskAssignmentEngine assignmentEngine;

    private Location location;
    private WorkTask queuedTask;

    @BeforeEach
    void setUp() {
        location = new Location("A", "01", "01", "01");
        queuedTask = createTask(TaskType.PICK, "ZONE-A");
    }

    @Test
    void getNextTaskForWorkerReturnsEmptyWhenNoTaskAvailable() {
        Worker worker = worker("WORKER-1", Set.of(TaskType.PICK));
        when(queueManager.dequeue(worker.workerId(), worker.warehouseId(), worker.currentZone(), worker.capabilities()))
                .thenReturn(Optional.empty());

        Optional<WorkTask> result = assignmentEngine.getNextTaskForWorker(worker);

        assertThat(result).isEmpty();
        verify(queueManager).dequeue(worker.workerId(), worker.warehouseId(), worker.currentZone(), worker.capabilities());
        verifyNoInteractions(taskService);
    }

    @Test
    void getNextTaskForWorkerAssignsDequeuedTask() {
        Worker worker = worker("WORKER-1", Set.of(TaskType.PICK));
        when(queueManager.dequeue(worker.workerId(), worker.warehouseId(), worker.currentZone(), worker.capabilities()))
                .thenReturn(Optional.of(queuedTask.getTaskId()));
        when(taskService.assignTask(queuedTask.getTaskId(), worker.workerId()))
                .thenReturn(queuedTask);

        Optional<WorkTask> result = assignmentEngine.getNextTaskForWorker(worker);

        assertThat(result).contains(queuedTask);
        verify(taskService).assignTask(queuedTask.getTaskId(), worker.workerId());
    }

    @Test
    void assignTaskToBestWorkerReturnsNoEligibleWhenCapabilitiesDoNotMatch() {
        Worker worker = worker("WORKER-1", Set.of(TaskType.PACK));

        AssignmentResult result = assignmentEngine.assignTaskToBestWorker(queuedTask, List.of(worker));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("No eligible workers available");
        verifyNoInteractions(taskService);
    }

    @Test
    void assignTaskToBestWorkerChoosesHighestScoreWorker() {
        Worker slowWorker = Worker.builder()
                .workerId("WORKER-1")
                .warehouseId("WH-001")
                .currentZone("ZONE-A")
                .capabilities(Set.of(TaskType.PICK))
                .specializations(Set.of())
                .activeTaskCount(2)
                .currentLocation(new Location("B", "05", "02", "01"))
                .performanceRating(0.2)
                .build();

        Worker fastWorker = Worker.builder()
                .workerId("WORKER-2")
                .warehouseId("WH-001")
                .currentZone("ZONE-A")
                .capabilities(Set.of(TaskType.PICK))
                .specializations(Set.of(TaskType.PICK))
                .activeTaskCount(0)
                .currentLocation(location)
                .performanceRating(0.9)
                .build();

        when(taskService.assignTask(queuedTask.getTaskId(), fastWorker.workerId()))
                .thenReturn(queuedTask);

        AssignmentResult result = assignmentEngine.assignTaskToBestWorker(queuedTask, List.of(slowWorker, fastWorker));

        assertThat(result.success()).isTrue();
        assertThat(result.workerId()).isEqualTo(fastWorker.workerId());
        verify(taskService).assignTask(queuedTask.getTaskId(), fastWorker.workerId());
    }

    @Test
    void assignTaskToBestWorkerReturnsFailedWhenAllAssignmentsThrow() {
        Worker worker = worker("WORKER-1", Set.of(TaskType.PICK));
        when(taskService.assignTask(queuedTask.getTaskId(), worker.workerId()))
                .thenThrow(new IllegalStateException("Cannot assign"));

        AssignmentResult result = assignmentEngine.assignTaskToBestWorker(queuedTask, List.of(worker));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("All assignment attempts failed");
        verify(taskService).assignTask(queuedTask.getTaskId(), worker.workerId());
    }

    @Test
    void getTaskRecommendationsSortsByScore() {
        Worker worker = worker("WORKER-1", Set.of(TaskType.PICK));

        WorkTask nearTask = createTask(TaskType.PICK, "ZONE-A");
        nearTask.setTaskLocation(worker.currentLocation());

        WorkTask farTask = createTask(TaskType.PICK, "ZONE-A");
        farTask.setTaskLocation(new Location("Z", "50", "10", "01"));

        List<TaskRecommendation> recommendations = assignmentEngine.getTaskRecommendations(
                worker,
                List.of(farTask, nearTask)
        );

        assertThat(recommendations).hasSize(2);
        assertThat(recommendations.get(0).taskId()).isEqualTo(nearTask.getTaskId());
        assertThat(recommendations.get(0).estimatedWalkTimeSeconds()).isEqualTo(0);
        assertThat(recommendations.get(1).estimatedWalkTimeSeconds()).isGreaterThan(0);
    }

    @Test
    void getNextTaskReEnqueuesWhenAssignmentFails() {
        Worker worker = worker("WORKER-1", Set.of(TaskType.PICK));
        when(queueManager.dequeue(worker.workerId(), worker.warehouseId(), worker.currentZone(), worker.capabilities()))
                .thenReturn(Optional.of(queuedTask.getTaskId()));
        when(taskService.assignTask(queuedTask.getTaskId(), worker.workerId()))
                .thenThrow(new IllegalStateException("Cannot assign"));
        when(taskService.findTaskById(queuedTask.getTaskId())).thenReturn(queuedTask);

        Optional<WorkTask> result = assignmentEngine.getNextTaskForWorker(worker);

        assertThat(result).isEmpty();
        verify(queueManager).enqueue(queuedTask);
    }

    private Worker worker(String workerId, Set<TaskType> capabilities) {
        return Worker.builder()
                .workerId(workerId)
                .warehouseId("WH-001")
                .currentZone("ZONE-A")
                .currentLocation(location)
                .capabilities(capabilities)
                .specializations(capabilities)
                .activeTaskCount(1)
                .performanceRating(0.8)
                .build();
    }

    private WorkTask createTask(TaskType type, String zone) {
        var instructions = List.of(new PickTaskContext.PickInstruction("SKU-1", 1, location, "LPN-1"));
        PickTaskContext context = new PickTaskContext("WAVE-1", "ORDER-1", PickTaskContext.PickStrategy.DISCRETE, instructions);

        WorkTask task = WorkTask.create(
                type,
                "WH-001",
                zone,
                location,
                Priority.HIGH,
                "REF-1",
                Duration.ofMinutes(15),
                LocalDateTime.now().plusHours(1),
                context
        );
        task.queue();
        return task;
    }
}
