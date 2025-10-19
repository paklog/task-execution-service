package com.paklog.wes.task.infrastructure.events;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.task.application.command.CreateTaskCommand;
import com.paklog.wes.task.application.service.TaskManagementService;
import com.paklog.wes.task.domain.entity.PickTaskContext;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Event handler for Wave events
 * Listens to wave-related events and generates tasks
 */
@Component
public class WaveEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(WaveEventHandler.class);

    private final TaskManagementService taskService;

    public WaveEventHandler(TaskManagementService taskService) {
        this.taskService = taskService;
    }

    /**
     * Handle WaveReleasedEvent from wave-planning-service
     * Generates pick tasks for each order in the wave
     */
    @KafkaListener(
            topics = "${paklog.kafka.topics.wave-events:wms-wave-events}",
            groupId = "${paklog.kafka.consumer.group-id:task-execution-service}"
    )
    public void handleWaveReleased(Map<String, Object> eventData) {
        try {
            String eventType = (String) eventData.get("type");

            if (!"WaveReleasedEvent".equals(eventType)) {
                return; // Ignore other event types
            }

            logger.info("Received WaveReleasedEvent: {}", eventData);

            String waveId = (String) eventData.get("waveId");
            String warehouseId = (String) eventData.get("warehouseId");
            String assignedZone = (String) eventData.get("assignedZone");
            String priorityStr = (String) eventData.get("priority");
            @SuppressWarnings("unchecked")
            List<String> orderIds = (List<String>) eventData.get("orderIds");

            Priority priority = priorityStr != null ? Priority.valueOf(priorityStr) : Priority.NORMAL;

            // Generate pick tasks for each order in the wave
            for (String orderId : orderIds) {
                createPickTask(waveId, orderId, warehouseId, assignedZone, priority);
            }

            logger.info("Created {} pick tasks for wave {}", orderIds.size(), waveId);

        } catch (Exception e) {
            logger.error("Error handling WaveReleasedEvent", e);
            // In production, you might want to publish to a dead letter queue
        }
    }

    private void createPickTask(String waveId, String orderId, String warehouseId,
                                String zone, Priority priority) {
        try {
            // Create pick task context
            // In a real implementation, you would fetch order details and create proper pick instructions
            var instructions = new ArrayList<PickTaskContext.PickInstruction>();

            // This is a simplified example - in production, you'd fetch actual SKUs and locations
            instructions.add(new PickTaskContext.PickInstruction(
                    "SKU-PLACEHOLDER",
                    1,
                    new Location(zone, "01", "01", "01"),
                    "LPN-" + orderId
            ));

            PickTaskContext context = new PickTaskContext(
                    waveId,
                    orderId,
                    PickTaskContext.PickStrategy.DISCRETE,
                    instructions
            );

            CreateTaskCommand command = new CreateTaskCommand(
                    TaskType.PICK,
                    warehouseId,
                    zone,
                    new Location(zone, "01", "01", "01"),
                    priority,
                    waveId,
                    Duration.ofMinutes(10), // Default estimated duration
                    null, // No deadline for now
                    context
            );

            taskService.createTask(command);

            logger.debug("Created pick task for order {} in wave {}", orderId, waveId);

        } catch (Exception e) {
            logger.error("Error creating pick task for order {} in wave {}", orderId, waveId, e);
        }
    }
}
