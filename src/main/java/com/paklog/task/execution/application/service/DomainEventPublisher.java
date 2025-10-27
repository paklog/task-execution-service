package com.paklog.task.execution.application.service;

import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.task.execution.events.TaskAssignedEvent;
import com.paklog.task.execution.events.TaskCompletedEvent;
import com.paklog.task.execution.events.TaskCreatedEvent;
import com.paklog.task.execution.events.TaskStartedEvent;
import com.paklog.task.execution.infrastructure.events.TaskEventPublisher;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

/**
 * Application service that converts domain events to CloudEvents and publishes them
 */
@Service
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);
    private static final String TASK_EVENTS_TOPIC = "task-events";

    private final TaskEventPublisher cloudEventPublisher;

    public DomainEventPublisher(TaskEventPublisher cloudEventPublisher) {
        this.cloudEventPublisher = cloudEventPublisher;
    }

    /**
     * Publish all domain events from a task as CloudEvents
     */
    public void publishDomainEvents(WorkTask task) {
        if (task == null || task.getDomainEvents().isEmpty()) {
            return;
        }

        task.getDomainEvents().forEach(domainEvent -> {
            try {
                publishDomainEvent(task, domainEvent);
            } catch (Exception e) {
                log.error("Failed to publish domain event: taskId={}, eventType={}",
                    task.getTaskId(), domainEvent.eventType(), e);
                // Don't throw - we don't want to break the transaction
            }
        });

        task.clearDomainEvents();
    }

    private void publishDomainEvent(WorkTask task, com.paklog.task.execution.domain.shared.DomainEvent domainEvent) {
        if (domainEvent instanceof com.paklog.wes.task.domain.event.TaskCreatedEvent) {
            publishTaskCreated(task);
        } else if (domainEvent instanceof com.paklog.wes.task.domain.event.TaskAssignedEvent) {
            publishTaskAssigned(task);
        } else if (domainEvent instanceof com.paklog.wes.task.domain.event.TaskCompletedEvent) {
            publishTaskCompleted(task);
        } else {
            log.debug("No CloudEvent mapping for domain event type: {}", domainEvent.getClass().getSimpleName());
        }
    }

    private void publishTaskCreated(WorkTask task) {
        TaskCreatedEvent event = new TaskCreatedEvent(
            task.getTaskId(),
            task.getReferenceId(), // waveId
            task.getReferenceId(), // orderId - in real scenario this would be extracted from context
            task.getType().name(),
            task.getPriority().name(),
            task.getZone(),
            task.getCreatedAt().toInstant(ZoneOffset.UTC),
            (int) (task.getEstimatedDuration() != null ? task.getEstimatedDuration().toSeconds() : 0)
        );

        cloudEventPublisher.publish(
            TASK_EVENTS_TOPIC,
            task.getTaskId(),
            TaskCreatedEvent.EVENT_TYPE,
            event
        );

        log.info("Published TaskCreatedEvent: taskId={}", task.getTaskId());
    }

    private void publishTaskAssigned(WorkTask task) {
        TaskAssignedEvent event = new TaskAssignedEvent(
            task.getTaskId(),
            task.getAssignedTo(),
            task.getAssignedAt().toInstant(ZoneOffset.UTC),
            task.getPriority().name(),
            task.getZone()
        );

        cloudEventPublisher.publish(
            TASK_EVENTS_TOPIC,
            task.getTaskId(),
            TaskAssignedEvent.EVENT_TYPE,
            event
        );

        log.info("Published TaskAssignedEvent: taskId={}, workerId={}", task.getTaskId(), task.getAssignedTo());
    }

    private void publishTaskCompleted(WorkTask task) {
        TaskCompletedEvent event = new TaskCompletedEvent(
            task.getTaskId(),
            task.getReferenceId(), // waveId
            task.getAssignedTo(),
            task.getCompletedAt().toInstant(ZoneOffset.UTC),
            task.getActualDuration() != null ? task.getActualDuration().toSeconds() : 0,
            0 // itemsProcessed - would be extracted from context in real scenario
        );

        cloudEventPublisher.publish(
            TASK_EVENTS_TOPIC,
            task.getTaskId(),
            TaskCompletedEvent.EVENT_TYPE,
            event
        );

        log.info("Published TaskCompletedEvent: taskId={}, duration={}s",
            task.getTaskId(), event.durationSeconds());
    }
}
