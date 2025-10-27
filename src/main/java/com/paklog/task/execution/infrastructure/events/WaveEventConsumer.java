package com.paklog.task.execution.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.task.execution.integration.contracts.WaveReleasedContract;
import com.paklog.wes.task.application.command.CreateTaskCommand;
import com.paklog.wes.task.application.service.TaskManagementService;
import com.paklog.wes.task.domain.entity.PickTaskContext;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskType;
import io.cloudevents.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * Consumer for wave events from wave-planning-service
 * Implements Anti-Corruption Layer pattern
 */
@Service
public class WaveEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(WaveEventConsumer.class);

    private final TaskManagementService taskManagementService;
    private final ObjectMapper objectMapper;

    public WaveEventConsumer(TaskManagementService taskManagementService, ObjectMapper objectMapper) {
        this.taskManagementService = taskManagementService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "wave-events", groupId = "task-execution-service")
    public void handleWaveEvent(CloudEvent cloudEvent) {
        String eventType = cloudEvent.getType();
        log.info("Received event: type={}, id={}", eventType, cloudEvent.getId());

        try {
            if (WaveReleasedContract.EVENT_TYPE.equals(eventType)) {
                handleWaveReleased(cloudEvent);
            } else {
                log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to handle wave event: type={}, id={}", eventType, cloudEvent.getId(), e);
            throw e; // Let Kafka retry mechanism handle it
        }
    }

    private void handleWaveReleased(CloudEvent cloudEvent) {
        try {
            WaveReleasedContract contract = objectMapper.readValue(
                cloudEvent.getData().toBytes(),
                WaveReleasedContract.class
            );

        log.info("Wave released: waveId={}, orders={}, priority={}",
            contract.waveId(), contract.orderIds().size(), contract.priority());

        // Anti-Corruption Layer: Map external priority to internal domain
        Priority internalPriority = mapPriority(contract.priority());

            // Create pick tasks for each order in the wave
            for (String orderId : contract.orderIds()) {
                createPickTaskForOrder(contract.waveId(), orderId, internalPriority, contract.zoneId());
            }
        } catch (Exception e) {
            log.error("Failed to process wave released event", e);
            throw new RuntimeException("Failed to process wave released event", e);
        }
    }

    private void createPickTaskForOrder(String waveId, String orderId, Priority priority, String zoneId) {
        // Create a basic pick task context
        // In a real implementation, this would fetch order details and create proper pick instructions
        PickTaskContext context = new PickTaskContext(
            waveId,
            orderId,
            PickTaskContext.PickStrategy.DISCRETE,
            Collections.emptyList() // Empty for now - would be populated with actual pick instructions
        );

        CreateTaskCommand command = new CreateTaskCommand(
            TaskType.PICK,
            "WH-001", // Default warehouse - should come from configuration or wave data
            zoneId,
            new Location("A", "01", "01", "01"), // Default location - should be determined by order details
            priority,
            waveId,
            Duration.ofMinutes(15), // Default duration - should be calculated based on order
            LocalDateTime.now().plusHours(2), // Default deadline - should come from wave or SLA
            context
        );

        taskManagementService.createTask(command);
        log.info("Created pick task for order: orderId={}, waveId={}, priority={}", orderId, waveId, priority);
    }

    /**
     * Anti-Corruption Layer: Map external priority model to internal domain
     */
    private Priority mapPriority(String externalPriority) {
        return switch (externalPriority) {
            case "URGENT" -> Priority.URGENT;
            case "HIGH" -> Priority.HIGH;
            case "NORMAL" -> Priority.NORMAL;
            case "LOW" -> Priority.LOW;
            case "CRITICAL" -> Priority.CRITICAL;
            default -> {
                log.warn("Unknown priority: {}, defaulting to NORMAL", externalPriority);
                yield Priority.NORMAL;
            }
        };
    }
}
