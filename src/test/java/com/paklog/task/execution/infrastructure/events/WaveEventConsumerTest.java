package com.paklog.task.execution.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.task.execution.integration.contracts.WaveReleasedContract;
import com.paklog.wes.task.application.command.CreateTaskCommand;
import com.paklog.wes.task.application.service.TaskManagementService;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.entity.PickTaskContext;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskType;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaveEventConsumerTest {

    @Mock
    private TaskManagementService taskManagementService;

    @Captor
    private ArgumentCaptor<CreateTaskCommand> commandCaptor;

    private WaveEventConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Register Java 8 time module
        consumer = new WaveEventConsumer(taskManagementService, objectMapper);
    }

    @Test
    void shouldCreateTasksWhenWaveReleasedEventIsReceived() throws Exception {
        // Given
        WaveReleasedContract contract = new WaveReleasedContract(
            "WAVE-001",
            "W-001",
            "HIGH",
            List.of("ORD-001", "ORD-002", "ORD-003"),
            100,
            "ZONE-A",
            Instant.now()
        );

        CloudEvent cloudEvent = CloudEventBuilder.v1()
            .withId("test-id")
            .withSource(URI.create("paklog://wave-planning-service"))
            .withType(WaveReleasedContract.EVENT_TYPE)
            .withDataContentType("application/json")
            .withData(objectMapper.writeValueAsBytes(contract))
            .build();

        PickTaskContext.PickInstruction instruction = new PickTaskContext.PickInstruction(
            "SKU-001", 5, new Location("A", "01", "01", "01"), "LPN-001"
        );

        WorkTask mockTask = WorkTask.create(
            TaskType.PICK,
            "WH-001",
            "ZONE-A",
            new Location("A", "01", "01", "01"),
            com.paklog.task.execution.domain.valueobject.Priority.HIGH,
            "WAVE-001",
            Duration.ofMinutes(15),
            LocalDateTime.now().plusHours(2),
            new PickTaskContext("WAVE-001", "ORD-001", PickTaskContext.PickStrategy.DISCRETE, List.of(instruction))
        );

        when(taskManagementService.createTask(any(CreateTaskCommand.class))).thenReturn(mockTask);

        // When
        consumer.handleWaveEvent(cloudEvent);

        // Then
        verify(taskManagementService, times(3)).createTask(commandCaptor.capture());

        List<CreateTaskCommand> commands = commandCaptor.getAllValues();
        assertThat(commands).hasSize(3);
        assertThat(commands).allMatch(cmd -> cmd.type() == TaskType.PICK);
        assertThat(commands).allMatch(cmd -> cmd.zone().equals("ZONE-A"));
        assertThat(commands).allMatch(cmd -> cmd.priority() == com.paklog.task.execution.domain.valueobject.Priority.HIGH);
    }

    @Test
    void shouldMapPriorityCorrectly() throws Exception {
        // Given - Test different priority values
        WaveReleasedContract urgentContract = new WaveReleasedContract(
            "WAVE-002",
            "W-002",
            "URGENT",
            List.of("ORD-004"),
            50,
            "ZONE-B",
            Instant.now()
        );

        CloudEvent cloudEvent = CloudEventBuilder.v1()
            .withId("test-id-2")
            .withSource(URI.create("paklog://wave-planning-service"))
            .withType(WaveReleasedContract.EVENT_TYPE)
            .withDataContentType("application/json")
            .withData(objectMapper.writeValueAsBytes(urgentContract))
            .build();

        PickTaskContext.PickInstruction instruction = new PickTaskContext.PickInstruction(
            "SKU-002", 3, new Location("B", "02", "02", "02"), "LPN-002"
        );

        WorkTask mockTask = WorkTask.create(
            TaskType.PICK,
            "WH-001",
            "ZONE-B",
            new Location("A", "01", "01", "01"),
            com.paklog.task.execution.domain.valueobject.Priority.URGENT,
            "WAVE-002",
            Duration.ofMinutes(15),
            LocalDateTime.now().plusHours(2),
            new PickTaskContext("WAVE-002", "ORD-004", PickTaskContext.PickStrategy.DISCRETE, List.of(instruction))
        );

        when(taskManagementService.createTask(any(CreateTaskCommand.class))).thenReturn(mockTask);

        // When
        consumer.handleWaveEvent(cloudEvent);

        // Then
        verify(taskManagementService).createTask(commandCaptor.capture());
        assertThat(commandCaptor.getValue().priority()).isEqualTo(com.paklog.task.execution.domain.valueobject.Priority.URGENT);
    }

    @Test
    void shouldIgnoreNonWaveReleasedEvents() throws Exception {
        // Given
        HashMap<String, String> otherEventData = new HashMap<>();
        otherEventData.put("wave_id", "WAVE-999");

        CloudEvent cloudEvent = CloudEventBuilder.v1()
            .withId("test-id-3")
            .withSource(URI.create("paklog://wave-planning-service"))
            .withType("com.paklog.wms.wave-planning.wave.wave.cancelled.v1")
            .withDataContentType("application/json")
            .withData(objectMapper.writeValueAsBytes(otherEventData))
            .build();

        // When
        consumer.handleWaveEvent(cloudEvent);

        // Then
        verify(taskManagementService, never()).createTask(any());
    }
}
