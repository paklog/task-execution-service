# Task Execution Service - Decoupling Plan
## Eliminate Dependencies on Shared Modules

**Service:** task-execution-service
**Complexity:** LOW (only domain primitives)
**Estimated Effort:** 3 hours
**Priority:** Phase 2 (after wave-planning-service)

---

## Current Dependencies

### Shared Modules Used
- ✓ **paklog-domain** (v0.0.1-SNAPSHOT)
  - `com.paklog.domain.annotation.AggregateRoot`
  - `com.paklog.domain.shared.DomainEvent`
  - `com.paklog.domain.valueobject.Priority`

### Coupling Impact
- Cannot deploy independently
- Breaking changes in shared modules affect this service
- Build requires paklog-domain artifact
- Testing requires paklog-domain dependency

---

## Target Architecture

### Service-Owned Components
```
task-execution-service/
├── src/main/java/com/paklog/task/execution/
│   ├── domain/
│   │   ├── shared/
│   │   │   ├── AggregateRoot.java        # Copy from paklog-domain
│   │   │   └── DomainEvent.java          # Copy from paklog-domain
│   │   │
│   │   └── valueobject/
│   │       └── Priority.java             # Copy from paklog-domain
│   │
│   ├── events/                            # Publisher-owned schemas
│   │   ├── TaskCreatedEvent.java
│   │   ├── TaskAssignedEvent.java
│   │   ├── TaskStartedEvent.java
│   │   └── TaskCompletedEvent.java
│   │
│   ├── integration/
│   │   └── contracts/                     # Consumer contracts
│   │       └── WaveReleasedContract.java  # From wave-planning
│   │
│   └── infrastructure/
│       └── events/
│           ├── TaskEventPublisher.java
│           └── WaveEventConsumer.java
```

---

## CloudEvents Schema Definition

### Event Type Pattern
All events MUST follow: `com.paklog.wes.task-execution.task.<entity>.<action>`

### Events Published by Task Execution Service

#### 1. Task Created Event
**Type:** `com.paklog.wes.task-execution.task.task.created.v1`
**Trigger:** New task is created from wave
**Schema:**
```json
{
  "task_id": "TASK-12345",
  "wave_id": "WAVE-001",
  "order_id": "ORD-123",
  "task_type": "PICK|PACK|SHIP|REPLENISH",
  "priority": "URGENT|HIGH|NORMAL|LOW",
  "zone_id": "ZONE-A1",
  "created_at": "2025-10-26T10:00:00Z",
  "estimated_duration_seconds": 300
}
```

#### 2. Task Assigned Event
**Type:** `com.paklog.wes.task-execution.task.task.assigned.v1`
**Trigger:** Task is assigned to a worker
**Schema:**
```json
{
  "task_id": "TASK-12345",
  "worker_id": "WORKER-456",
  "assigned_at": "2025-10-26T10:01:00Z",
  "priority": "HIGH",
  "zone_id": "ZONE-A1"
}
```

#### 3. Task Started Event
**Type:** `com.paklog.wes.task-execution.task.task.started.v1`
**Trigger:** Worker starts executing the task
**Schema:**
```json
{
  "task_id": "TASK-12345",
  "worker_id": "WORKER-456",
  "started_at": "2025-10-26T10:02:00Z",
  "estimated_completion_time": "2025-10-26T10:07:00Z"
}
```

#### 4. Task Completed Event
**Type:** `com.paklog.wes.task-execution.task.task.completed.v1`
**Trigger:** Task is successfully completed
**Schema:**
```json
{
  "task_id": "TASK-12345",
  "wave_id": "WAVE-001",
  "worker_id": "WORKER-456",
  "completed_at": "2025-10-26T10:06:30Z",
  "duration_seconds": 270,
  "items_processed": 15
}
```

### Events Consumed by Task Execution Service

#### Wave Released Event (from wave-planning-service)
**Type:** `com.paklog.wms.wave-planning.wave.wave.released.v1`
**Purpose:** Creates tasks when wave is released
**Contract:**
```json
{
  "wave_id": "WAVE-001",
  "wave_number": "W-001",
  "priority": "HIGH",
  "order_ids": ["ORD-001", "ORD-002"],
  "released_at": "2025-10-26T10:00:00Z"
}
```

---

## Step-by-Step Migration Tasks

### Phase 1: Preparation (15 minutes)

#### Task 1.1: Create Feature Branch
```bash
cd task-execution-service
git checkout -b decouple/remove-shared-dependencies
```

#### Task 1.2: Run Baseline Tests
```bash
mvn clean test
# Document current test results and coverage
```

#### Task 1.3: Create Service-Internal Packages
```bash
mkdir -p src/main/java/com/paklog/task/execution/domain/shared
mkdir -p src/main/java/com/paklog/task/execution/domain/valueobject
mkdir -p src/main/java/com/paklog/task/execution/events
mkdir -p src/main/java/com/paklog/task/execution/integration/contracts
mkdir -p src/main/java/com/paklog/task/execution/infrastructure/events
```

---

### Phase 2: Internalize Domain Primitives (1 hour)

#### Task 2.1: Copy AggregateRoot Annotation
```bash
# Copy file
cp ../../paklog-domain/src/main/java/com/paklog/domain/annotation/AggregateRoot.java \
   src/main/java/com/paklog/task/execution/domain/shared/AggregateRoot.java
```

**Update package declaration:**
```java
// File: src/main/java/com/paklog/task/execution/domain/shared/AggregateRoot.java
package com.paklog.task.execution.domain.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for aggregate roots in Task Execution bounded context
 * Copied from paklog-domain to eliminate compilation dependency
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AggregateRoot {
}
```

#### Task 2.2: Copy DomainEvent Interface
```bash
# Copy file
cp ../../paklog-domain/src/main/java/com/paklog/domain/shared/DomainEvent.java \
   src/main/java/com/paklog/task/execution/domain/shared/DomainEvent.java
```

**Update package declaration:**
```java
// File: src/main/java/com/paklog/task/execution/domain/shared/DomainEvent.java
package com.paklog.task.execution.domain.shared;

import java.time.Instant;

/**
 * Base interface for all domain events in Task Execution bounded context
 * Copied from paklog-domain to eliminate compilation dependency
 */
public interface DomainEvent {

    /**
     * When the event occurred
     */
    Instant occurredOn();

    /**
     * Type of the event
     */
    String eventType();
}
```

#### Task 2.3: Copy Priority Value Object
```bash
# Copy file
cp ../../paklog-domain/src/main/java/com/paklog/domain/valueobject/Priority.java \
   src/main/java/com/paklog/task/execution/domain/valueobject/Priority.java
```

**Update package declaration:**
```java
// File: src/main/java/com/paklog/task/execution/domain/valueobject/Priority.java
package com.paklog.task.execution.domain.valueobject;

/**
 * Task priority levels
 * Copied from paklog-domain to eliminate compilation dependency
 */
public enum Priority {
    URGENT,   // Process immediately
    HIGH,     // Process soon
    NORMAL,   // Standard processing
    LOW;      // Process when capacity available

    /**
     * Parse priority from string (case-insensitive)
     */
    public static Priority fromString(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        try {
            return Priority.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
```

#### Task 2.4: Update All Imports
```bash
# Find and replace imports across the codebase
find src/main/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.annotation\.AggregateRoot/import com.paklog.task.execution.domain.shared.AggregateRoot/g' {} +

find src/main/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.shared\.DomainEvent/import com.paklog.task.execution.domain.shared.DomainEvent/g' {} +

find src/main/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.valueobject\.Priority/import com.paklog.task.execution.domain.valueobject.Priority/g' {} +

# Also update test files
find src/test/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.annotation\.AggregateRoot/import com.paklog.task.execution.domain.shared.AggregateRoot/g' {} +

find src/test/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.shared\.DomainEvent/import com.paklog.task.execution.domain.shared.DomainEvent/g' {} +

find src/test/java -name "*.java" -type f -exec sed -i '' \
  's/import com\.paklog\.domain\.valueobject\.Priority/import com.paklog.task.execution.domain.valueobject.Priority/g' {} +
```

#### Task 2.5: Remove paklog-domain Dependency
**Edit pom.xml:**
```xml
<!-- DELETE THIS DEPENDENCY -->
<!--
<dependency>
    <groupId>com.paklog.common</groupId>
    <artifactId>paklog-domain</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
-->
```

#### Task 2.6: Verify Compilation
```bash
mvn clean compile
# Should succeed without paklog-domain dependency
```

---

### Phase 3: Define Event Schemas (1.5 hours)

#### Task 3.1: Create TaskCreatedEvent

**File:** `src/main/java/com/paklog/task/execution/events/TaskCreatedEvent.java`

```java
package com.paklog.task.execution.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when a new task is created
 * CloudEvent Type: com.paklog.wes.task-execution.task.task.created.v1
 */
public record TaskCreatedEvent(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("order_id") String orderId,
    @JsonProperty("task_type") String taskType,
    @JsonProperty("priority") String priority,
    @JsonProperty("zone_id") String zoneId,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("estimated_duration_seconds") int estimatedDurationSeconds
) {
    public static final String EVENT_TYPE = "com.paklog.wes.task-execution.task.task.created.v1";
}
```

#### Task 3.2: Create TaskAssignedEvent

**File:** `src/main/java/com/paklog/task/execution/events/TaskAssignedEvent.java`

```java
package com.paklog.task.execution.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when task is assigned to a worker
 * CloudEvent Type: com.paklog.wes.task-execution.task.task.assigned.v1
 */
public record TaskAssignedEvent(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("assigned_at") Instant assignedAt,
    @JsonProperty("priority") String priority,
    @JsonProperty("zone_id") String zoneId
) {
    public static final String EVENT_TYPE = "com.paklog.wes.task-execution.task.task.assigned.v1";
}
```

#### Task 3.3: Create TaskStartedEvent

**File:** `src/main/java/com/paklog/task/execution/events/TaskStartedEvent.java`

```java
package com.paklog.task.execution.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when worker starts executing task
 * CloudEvent Type: com.paklog.wes.task-execution.task.task.started.v1
 */
public record TaskStartedEvent(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("started_at") Instant startedAt,
    @JsonProperty("estimated_completion_time") Instant estimatedCompletionTime
) {
    public static final String EVENT_TYPE = "com.paklog.wes.task-execution.task.task.started.v1";
}
```

#### Task 3.4: Create TaskCompletedEvent

**File:** `src/main/java/com/paklog/task/execution/events/TaskCompletedEvent.java`

```java
package com.paklog.task.execution.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Event published when task is successfully completed
 * CloudEvent Type: com.paklog.wes.task-execution.task.task.completed.v1
 */
public record TaskCompletedEvent(
    @JsonProperty("task_id") String taskId,
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("worker_id") String workerId,
    @JsonProperty("completed_at") Instant completedAt,
    @JsonProperty("duration_seconds") long durationSeconds,
    @JsonProperty("items_processed") int itemsProcessed
) {
    public static final String EVENT_TYPE = "com.paklog.wes.task-execution.task.task.completed.v1";
}
```

#### Task 3.5: Create Wave Released Contract (Anti-Corruption Layer)

**File:** `src/main/java/com/paklog/task/execution/integration/contracts/WaveReleasedContract.java`

```java
package com.paklog.task.execution.integration.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Contract for WaveReleasedEvent from wave-planning-service
 * This is the consumer's view of the external event
 * Maps external event to internal domain (Anti-Corruption Layer)
 */
public record WaveReleasedContract(
    @JsonProperty("wave_id") String waveId,
    @JsonProperty("wave_number") String waveNumber,
    @JsonProperty("priority") String priority,
    @JsonProperty("order_ids") List<String> orderIds,
    @JsonProperty("total_lines") int totalLines,
    @JsonProperty("zone_id") String zoneId,
    @JsonProperty("released_at") Instant releasedAt
) {
    /**
     * Expected CloudEvent type from wave-planning-service
     */
    public static final String EVENT_TYPE = "com.paklog.wms.wave-planning.wave.wave.released.v1";
}
```

#### Task 3.6: Create CloudEvents Publisher

**File:** `src/main/java/com/paklog/task/execution/infrastructure/events/TaskEventPublisher.java`

```java
package com.paklog.task.execution.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Publisher for Task Execution events using CloudEvents format
 */
@Service
public class TaskEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TaskEventPublisher.class);
    private static final String SOURCE = "paklog://task-execution-service";

    private final KafkaTemplate<String, CloudEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public TaskEventPublisher(KafkaTemplate<String, CloudEvent> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish event to Kafka
     */
    public void publish(String topic, String key, String eventType, Object eventData) {
        try {
            CloudEvent cloudEvent = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create(SOURCE))
                .withType(eventType)
                .withDataContentType("application/json")
                .withTime(OffsetDateTime.now())
                .withData(objectMapper.writeValueAsBytes(eventData))
                .build();

            kafkaTemplate.send(topic, key, cloudEvent);
            log.info("Event published: type={}, key={}, topic={}", eventType, key, topic);
        } catch (Exception e) {
            log.error("Failed to publish event: type={}, key={}", eventType, key, e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}
```

#### Task 3.7: Create Wave Event Consumer

**File:** `src/main/java/com/paklog/task/execution/infrastructure/events/WaveEventConsumer.java`

```java
package com.paklog.task.execution.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.task.execution.application.usecases.CreateTasksFromWaveUseCase;
import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.task.execution.integration.contracts.WaveReleasedContract;
import io.cloudevents.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumer for wave events from wave-planning-service
 * Implements Anti-Corruption Layer pattern
 */
@Service
public class WaveEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(WaveEventConsumer.class);

    private final CreateTasksFromWaveUseCase createTasksUseCase;
    private final ObjectMapper objectMapper;

    public WaveEventConsumer(CreateTasksFromWaveUseCase createTasksUseCase, ObjectMapper objectMapper) {
        this.createTasksUseCase = createTasksUseCase;
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

    private void handleWaveReleased(CloudEvent cloudEvent) throws Exception {
        WaveReleasedContract contract = objectMapper.readValue(
            cloudEvent.getData().toBytes(),
            WaveReleasedContract.class
        );

        log.info("Wave released: waveId={}, orders={}, priority={}",
            contract.waveId(), contract.orderIds().size(), contract.priority());

        // Anti-Corruption Layer: Map external priority to internal domain
        Priority internalPriority = mapPriority(contract.priority());

        // Execute domain logic
        createTasksUseCase.execute(
            contract.waveId(),
            contract.orderIds(),
            internalPriority,
            contract.zoneId()
        );
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
            default -> {
                log.warn("Unknown priority: {}, defaulting to NORMAL", externalPriority);
                yield Priority.NORMAL;
            }
        };
    }
}
```

---

### Phase 4: Testing & Validation (30 minutes)

#### Task 4.1: Update Unit Tests
```bash
# Run all unit tests
mvn test

# Fix any failing tests due to package changes
```

#### Task 4.2: Create Event Publishing Test

**File:** `src/test/java/com/paklog/task/execution/infrastructure/events/TaskEventPublisherTest.java`

```java
package com.paklog.task.execution.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.task.execution.events.TaskCreatedEvent;
import io.cloudevents.CloudEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskEventPublisherTest {

    @Mock
    private KafkaTemplate<String, CloudEvent> kafkaTemplate;

    @InjectMocks
    private TaskEventPublisher publisher;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPublishTaskCreatedEvent() throws Exception {
        // Given
        publisher = new TaskEventPublisher(kafkaTemplate, objectMapper);
        TaskCreatedEvent event = new TaskCreatedEvent(
            "TASK-001",
            "WAVE-001",
            "ORD-001",
            "PICK",
            "HIGH",
            "ZONE-A1",
            Instant.now(),
            300
        );

        // When
        publisher.publish("task-events", event.taskId(), TaskCreatedEvent.EVENT_TYPE, event);

        // Then
        ArgumentCaptor<CloudEvent> eventCaptor = ArgumentCaptor.forClass(CloudEvent.class);
        verify(kafkaTemplate).send(eq("task-events"), eq("TASK-001"), eventCaptor.capture());

        CloudEvent cloudEvent = eventCaptor.getValue();
        assertThat(cloudEvent.getType()).isEqualTo(TaskCreatedEvent.EVENT_TYPE);
        assertThat(cloudEvent.getSource().toString()).isEqualTo("paklog://task-execution-service");
    }
}
```

#### Task 4.3: Create Consumer Test

**File:** `src/test/java/com/paklog/task/execution/infrastructure/events/WaveEventConsumerTest.java`

```java
package com.paklog.task.execution.infrastructure.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.task.execution.application.usecases.CreateTasksFromWaveUseCase;
import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.task.execution.integration.contracts.WaveReleasedContract;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WaveEventConsumerTest {

    @Mock
    private CreateTasksFromWaveUseCase createTasksUseCase;

    @InjectMocks
    private WaveEventConsumer consumer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldHandleWaveReleasedEvent() throws Exception {
        // Given
        consumer = new WaveEventConsumer(createTasksUseCase, objectMapper);

        WaveReleasedContract contract = new WaveReleasedContract(
            "WAVE-001",
            "W-001",
            "HIGH",
            List.of("ORD-1", "ORD-2"),
            100,
            "ZONE-A1",
            Instant.now()
        );

        CloudEvent cloudEvent = CloudEventBuilder.v1()
            .withId("test-id")
            .withSource(URI.create("paklog://wave-planning-service"))
            .withType(WaveReleasedContract.EVENT_TYPE)
            .withData(objectMapper.writeValueAsBytes(contract))
            .build();

        // When
        consumer.handleWaveEvent(cloudEvent);

        // Then
        verify(createTasksUseCase).execute(
            eq("WAVE-001"),
            eq(List.of("ORD-1", "ORD-2")),
            eq(Priority.HIGH),
            eq("ZONE-A1")
        );
    }
}
```

#### Task 4.4: Run Full Test Suite
```bash
mvn clean verify

# Should show:
# - All unit tests passing
# - All integration tests passing
# - Code coverage maintained
```

#### Task 4.5: Build Service Independently
```bash
mvn clean package -DskipTests

# Should succeed and produce:
# target/task-execution-service-0.0.1-SNAPSHOT.jar
```

#### Task 4.6: Run Service Locally
```bash
mvn spring-boot:run

# Verify:
# - Application starts successfully
# - Kafka consumer connects
# - No errors in logs
# - Health endpoint returns UP
```

---

## Validation Checklist

- [ ] No compilation errors after removing paklog-domain
- [ ] All unit tests passing (mvn test)
- [ ] All integration tests passing (mvn verify)
- [ ] Code coverage maintained (≥80%)
- [ ] Service builds independently (mvn package)
- [ ] Service runs without errors (mvn spring-boot:run)
- [ ] CloudEvents published with correct types
- [ ] Wave events consumed successfully
- [ ] Anti-corruption layer working (priority mapping)
- [ ] No references to com.paklog.domain.* packages
- [ ] Documentation updated (README.md)

---

## Rollback Plan

If issues occur during migration:

```bash
# Revert all changes
git checkout main
git branch -D decouple/remove-shared-dependencies

# Restore paklog-domain dependency in pom.xml
```

---

## Post-Decoupling Tasks

1. **Monitor Production**
   - Track event consumption lag
   - Monitor task creation from wave events
   - Check for processing errors

2. **Performance Validation**
   - Measure event processing latency
   - Monitor Kafka consumer lag
   - Check task creation throughput

3. **Documentation**
   - Update architecture diagrams
   - Document event contracts
   - Update API documentation

---

## Success Criteria

- ✅ Zero dependencies on paklog-domain
- ✅ Service builds in < 1 minute independently
- ✅ All tests passing with ≥80% coverage
- ✅ Events published with correct CloudEvents types
- ✅ Wave events consumed with anti-corruption layer
- ✅ Production deployment successful

---

**Estimated Total Time:** 3 hours
**Complexity:** LOW
**Risk Level:** LOW (simple domain primitives only)
