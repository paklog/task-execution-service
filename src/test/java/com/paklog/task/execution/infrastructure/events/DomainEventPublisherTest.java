package com.paklog.task.execution.infrastructure.events;

import com.paklog.task.execution.application.service.DomainEventPublisher;
import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.task.execution.events.TaskAssignedEvent;
import com.paklog.task.execution.events.TaskCompletedEvent;
import com.paklog.task.execution.events.TaskCreatedEvent;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.entity.PickTaskContext;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskType;
import io.cloudevents.CloudEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DomainEventPublisherTest {

    @Mock
    private TaskEventPublisher taskEventPublisher;

    private DomainEventPublisher domainEventPublisher;

    @BeforeEach
    void setUp() {
        domainEventPublisher = new DomainEventPublisher(taskEventPublisher);
    }

    @Test
    void shouldPublishTaskCreatedEventWhenTaskIsCreated() {
        // Given
        PickTaskContext.PickInstruction instruction = new PickTaskContext.PickInstruction(
            "SKU-001", 5, new Location("A", "01", "01", "01"), "LPN-001"
        );

        WorkTask task = WorkTask.create(
            TaskType.PICK,
            "WH-001",
            "ZONE-A",
            new Location("A", "01", "01", "01"),
            Priority.HIGH,
            "WAVE-001",
            Duration.ofMinutes(15),
            LocalDateTime.now().plusHours(2),
            new PickTaskContext("WAVE-001", "ORD-001", PickTaskContext.PickStrategy.DISCRETE, List.of(instruction))
        );

        // When
        domainEventPublisher.publishDomainEvents(task);

        // Then
        verify(taskEventPublisher).publish(
            eq("task-events"),
            eq(task.getTaskId()),
            eq(TaskCreatedEvent.EVENT_TYPE),
            any(TaskCreatedEvent.class)
        );
        assertThat(task.getDomainEvents()).isEmpty(); // Events should be cleared after publishing
    }

    @Test
    void shouldPublishTaskAssignedEventWhenTaskIsAssigned() {
        // Given
        PickTaskContext.PickInstruction instruction = new PickTaskContext.PickInstruction(
            "SKU-001", 5, new Location("A", "01", "01", "01"), "LPN-001"
        );

        WorkTask task = WorkTask.create(
            TaskType.PICK,
            "WH-001",
            "ZONE-A",
            new Location("A", "01", "01", "01"),
            Priority.HIGH,
            "WAVE-001",
            Duration.ofMinutes(15),
            LocalDateTime.now().plusHours(2),
            new PickTaskContext("WAVE-001", "ORD-001", PickTaskContext.PickStrategy.DISCRETE, List.of(instruction))
        );
        task.clearDomainEvents(); // Clear creation event
        task.queue();
        task.assign("WORKER-123");

        // When
        domainEventPublisher.publishDomainEvents(task);

        // Then
        verify(taskEventPublisher).publish(
            eq("task-events"),
            eq(task.getTaskId()),
            eq(TaskAssignedEvent.EVENT_TYPE),
            any(TaskAssignedEvent.class)
        );
    }

    @Test
    void shouldPublishTaskCompletedEventWhenTaskIsCompleted() {
        // Given
        PickTaskContext.PickInstruction instruction = new PickTaskContext.PickInstruction(
            "SKU-001", 5, new Location("A", "01", "01", "01"), "LPN-001"
        );

        WorkTask task = WorkTask.create(
            TaskType.PICK,
            "WH-001",
            "ZONE-A",
            new Location("A", "01", "01", "01"),
            Priority.HIGH,
            "WAVE-001",
            Duration.ofMinutes(15),
            LocalDateTime.now().plusHours(2),
            new PickTaskContext("WAVE-001", "ORD-001", PickTaskContext.PickStrategy.DISCRETE, List.of(instruction))
        );
        task.clearDomainEvents();
        task.queue();
        task.assign("WORKER-123");
        task.accept();
        task.start();
        task.complete();

        // When
        domainEventPublisher.publishDomainEvents(task);

        // Then
        verify(taskEventPublisher).publish(
            eq("task-events"),
            eq(task.getTaskId()),
            eq(TaskCompletedEvent.EVENT_TYPE),
            any(TaskCompletedEvent.class)
        );
    }

    @Test
    void shouldNotFailWhenTaskHasNoEvents() {
        // Given
        PickTaskContext.PickInstruction instruction = new PickTaskContext.PickInstruction(
            "SKU-001", 5, new Location("A", "01", "01", "01"), "LPN-001"
        );

        WorkTask task = WorkTask.create(
            TaskType.PICK,
            "WH-001",
            "ZONE-A",
            new Location("A", "01", "01", "01"),
            Priority.HIGH,
            "WAVE-001",
            Duration.ofMinutes(15),
            LocalDateTime.now().plusHours(2),
            new PickTaskContext("WAVE-001", "ORD-001", PickTaskContext.PickStrategy.DISCRETE, List.of(instruction))
        );
        task.clearDomainEvents();

        // When
        domainEventPublisher.publishDomainEvents(task);

        // Then
        verify(taskEventPublisher, never()).publish(anyString(), anyString(), anyString(), any());
    }
}
