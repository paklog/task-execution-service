package com.paklog.wes.task.infrastructure.events;

import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.wes.task.application.command.CreateTaskCommand;
import com.paklog.wes.task.application.service.TaskManagementService;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.entity.PickTaskContext;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WaveEventHandlerTest {

    private TaskManagementService taskManagementService;
    private WaveEventHandler handler;

    @BeforeEach
    void setUp() {
        taskManagementService = mock(TaskManagementService.class);
        handler = new WaveEventHandler(taskManagementService);
    }

    @Test
    void handleWaveReleasedCreatesPickTasksForEachOrder() {
        Map<String, Object> event = new HashMap<>();
        event.put("type", "WaveReleasedEvent");
        event.put("waveId", "WAVE-10");
        event.put("warehouseId", "WH-1");
        event.put("assignedZone", "ZONE-A");
        event.put("priority", "CRITICAL");
        event.put("orderIds", List.of("ORDER-1", "ORDER-2"));

        when(taskManagementService.createTask(any(CreateTaskCommand.class)))
                .thenReturn(sampleTask("ORDER-1"))
                .thenReturn(sampleTask("ORDER-2"));

        handler.handleWaveReleased(event);

        ArgumentCaptor<CreateTaskCommand> commandCaptor = ArgumentCaptor.forClass(CreateTaskCommand.class);
        verify(taskManagementService, times(2)).createTask(commandCaptor.capture());

        List<CreateTaskCommand> commands = commandCaptor.getAllValues();
        assertThat(commands)
                .hasSize(2)
                .allMatch(cmd -> cmd.type() == TaskType.PICK && cmd.warehouseId().equals("WH-1"));
        assertThat(commands.get(0).referenceId()).isEqualTo("WAVE-10");
        assertThat(commands.stream().map(CreateTaskCommand::context))
                .allMatch(ctx -> ctx instanceof PickTaskContext);
    }

    @Test
    void handleWaveReleasedIgnoresDifferentEventType() {
        Map<String, Object> event = Map.of("type", "WaveCancelledEvent");

        handler.handleWaveReleased(event);

        verifyNoInteractions(taskManagementService);
    }

    private WorkTask sampleTask(String orderId) {
        Location location = new Location("A", "01", "01", "01");
        var instructions = List.of(new PickTaskContext.PickInstruction("SKU-1", 1, location, "LPN"));
        PickTaskContext context = new PickTaskContext("WAVE-10", orderId, PickTaskContext.PickStrategy.DISCRETE, instructions);

        WorkTask task = WorkTask.create(
                TaskType.PICK,
                "WH-1",
                "ZONE-A",
                location,
                Priority.CRITICAL,
                orderId,
                Duration.ofMinutes(10),
                LocalDateTime.now().plusHours(1),
                context
        );
        task.queue();
        return task;
    }
}
