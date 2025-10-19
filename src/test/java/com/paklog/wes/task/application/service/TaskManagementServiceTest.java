package com.paklog.wes.task.application.service;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.task.application.command.CreateTaskCommand;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.entity.PickTaskContext;
import com.paklog.wes.task.domain.repository.WorkTaskRepository;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskStatus;
import com.paklog.wes.task.domain.valueobject.TaskType;
import com.paklog.wes.task.infrastructure.queue.TaskQueueManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskManagementServiceTest {

    @Mock
    private WorkTaskRepository taskRepository;

    @Mock
    private TaskQueueManager queueManager;

    @InjectMocks
    private TaskManagementService service;

    private WorkTask task;
    private PickTaskContext context;
    private Location location;

    @BeforeEach
    void setUp() {
        location = new Location("A", "01", "01", "01");
        var instructions = List.of(new PickTaskContext.PickInstruction("SKU-1", 1, location, "LPN-1"));
        context = new PickTaskContext("WAVE-1", "ORDER-1", PickTaskContext.PickStrategy.DISCRETE, instructions);
        task = WorkTask.create(TaskType.PICK, "WH-1", "ZONE-A", location, Priority.HIGH,
                "REF-1", Duration.ofMinutes(15), LocalDateTime.now().plusHours(1), context);
        lenient().when(taskRepository.save(any(WorkTask.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createTaskQueuesAndEnqueues() {
        CreateTaskCommand command = new CreateTaskCommand(TaskType.PICK, "WH-1", "ZONE-A", location,
                Priority.HIGH, "REF-1", Duration.ofMinutes(10), LocalDateTime.now().plusHours(2), context);

        WorkTask created = service.createTask(command);

        assertThat(created.getStatus()).isEqualTo(TaskStatus.QUEUED);
        verify(queueManager).enqueue(created);
    }

    @Test
    void assignTaskRemovesFromQueue() {
        task.queue();
        when(taskRepository.findById(task.getTaskId())).thenReturn(Optional.of(task));

        service.assignTask(task.getTaskId(), "WORKER-1");

        verify(queueManager).remove(task);
        verify(taskRepository).save(task);
    }

    @Test
    void rejectTaskRequeuesAssignment() {
        task.queue();
        task.assign("WORKER-1");
        when(taskRepository.findById(task.getTaskId())).thenReturn(Optional.of(task));

        service.rejectTask(task.getTaskId(), "Busy");

        verify(queueManager).enqueue(task);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.QUEUED);
    }

    @Test
    void completeTaskPersistsUpdates() {
        task.queue();
        task.assign("WORKER-1");
        task.accept();
        task.start();
        when(taskRepository.findById(task.getTaskId())).thenReturn(Optional.of(task));

        WorkTask completed = service.completeTask(task.getTaskId());

        assertThat(completed.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        verify(taskRepository).save(task);
    }

    @Test
    void cancelTaskRemovesFromQueue() {
        task.queue();
        when(taskRepository.findById(task.getTaskId())).thenReturn(Optional.of(task));

        service.cancelTask(task.getTaskId(), "Cancelled");

        verify(queueManager).remove(task);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
    }

    @Test
    void failTaskRecordsReason() {
        task.queue();
        task.assign("WORKER-1");
        task.accept();
        task.start();
        when(taskRepository.findById(task.getTaskId())).thenReturn(Optional.of(task));

        WorkTask failed = service.failTask(task.getTaskId(), "Equipment");

        assertThat(failed.getFailureReason()).isEqualTo("Equipment");
        assertThat(failed.getStatus()).isEqualTo(TaskStatus.FAILED);
    }

    @Test
    void acceptTaskTransitionsStatus() {
        task.queue();
        task.assign("WORKER-1");
        when(taskRepository.findById(task.getTaskId())).thenReturn(Optional.of(task));

        WorkTask accepted = service.acceptTask(task.getTaskId());

        assertThat(accepted.getStatus()).isEqualTo(TaskStatus.ACCEPTED);
        verify(taskRepository).save(task);
    }

    @Test
    void startTaskUpdatesProgress() {
        task.queue();
        task.assign("WORKER-1");
        task.accept();
        when(taskRepository.findById(task.getTaskId())).thenReturn(Optional.of(task));

        WorkTask inProgress = service.startTask(task.getTaskId());

        assertThat(inProgress.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        verify(taskRepository).save(task);
    }

    @Test
    void findTaskByIdThrowsWhenMissing() {
        when(taskRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findTaskById("missing"))
                .isInstanceOf(TaskManagementService.TaskNotFoundException.class);
    }

    @Test
    void findersDelegateToRepository() {
        when(taskRepository.findByStatus(TaskStatus.QUEUED)).thenReturn(List.of(task));
        when(taskRepository.findByAssignedTo("WORKER-1")).thenReturn(List.of(task));
        when(taskRepository.findByTypeAndStatus(TaskType.PICK, TaskStatus.QUEUED)).thenReturn(List.of(task));
        when(taskRepository.findQueuedTasksByZone("WH-1", "ZONE-A")).thenReturn(List.of(task));
        when(taskRepository.findByZoneAndStatus("ZONE-A", TaskStatus.QUEUED)).thenReturn(List.of(task));
        when(taskRepository.findByReferenceId("REF-1")).thenReturn(List.of(task));
        when(taskRepository.countActiveTasksByWorker("WORKER-1")).thenReturn(2L);
        when(taskRepository.findOverdueTasks(any())).thenReturn(List.of(task));

        assertThat(service.findTasksByStatus(TaskStatus.QUEUED)).hasSize(1);
        assertThat(service.findTasksByWorker("WORKER-1")).hasSize(1);
        assertThat(service.findTasksByTypeAndStatus(TaskType.PICK, TaskStatus.QUEUED)).hasSize(1);
        assertThat(service.findQueuedTasksByZone("WH-1", "ZONE-A")).hasSize(1);
        assertThat(service.findTasksByZoneAndStatus("ZONE-A", TaskStatus.QUEUED)).hasSize(1);
        assertThat(service.findTasksByReference("REF-1")).hasSize(1);
        assertThat(service.countActiveTasksByWorker("WORKER-1")).isEqualTo(2L);
        assertThat(service.findOverdueTasks()).hasSize(1);

        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(taskRepository).findOverdueTasks(nowCaptor.capture());
        assertThat(nowCaptor.getValue()).isNotNull();
    }
}
