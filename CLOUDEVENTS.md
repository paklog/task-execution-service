# CloudEvents Event Catalog

This document describes all CloudEvents published and consumed by the task-execution-service.

## Overview

The task-execution-service uses CloudEvents specification v1.0 for all event-driven communication. Events are published to and consumed from Apache Kafka topics.

### CloudEvents Attributes

All events include the following standard CloudEvents attributes:

- `id`: Unique identifier for the event (UUID)
- `source`: URI identifying the producer (`paklog://task-execution-service`)
- `type`: Event type identifier (see below)
- `datacontenttype`: Always `application/json`
- `time`: RFC3339 timestamp when the event occurred
- `data`: JSON payload with event-specific data

## Events Published

### 1. Task Created Event

**Type:** `com.paklog.wes.task-execution.task.task.created.v1`

**Description:** Published when a new task is created from a released wave.

**Trigger:** Task creation via TaskManagementService

**Topic:** `task-events`

**Schema:**
```json
{
  "task_id": "TASK-12345",
  "wave_id": "WAVE-001",
  "order_id": "ORD-123",
  "task_type": "PICK|PACK|SHIP|REPLENISH|MOVE|COUNT|PUTAWAY",
  "priority": "CRITICAL|URGENT|HIGH|NORMAL|LOW",
  "zone_id": "ZONE-A1",
  "created_at": "2025-10-26T10:00:00Z",
  "estimated_duration_seconds": 300
}
```

**Field Descriptions:**
- `task_id`: Unique identifier for the task
- `wave_id`: Reference to the wave that generated this task
- `order_id`: Reference to the order being processed
- `task_type`: Type of warehouse task
- `priority`: Task priority level
- `zone_id`: Warehouse zone where task should be executed
- `created_at`: Timestamp when task was created (ISO 8601)
- `estimated_duration_seconds`: Expected duration to complete the task

**Example CloudEvent:**
```json
{
  "specversion": "1.0",
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "source": "paklog://task-execution-service",
  "type": "com.paklog.wes.task-execution.task.task.created.v1",
  "datacontenttype": "application/json",
  "time": "2025-10-26T10:00:00.000Z",
  "data": {
    "task_id": "TASK-12345",
    "wave_id": "WAVE-001",
    "order_id": "ORD-123",
    "task_type": "PICK",
    "priority": "HIGH",
    "zone_id": "ZONE-A1",
    "created_at": "2025-10-26T10:00:00Z",
    "estimated_duration_seconds": 300
  }
}
```

---

### 2. Task Assigned Event

**Type:** `com.paklog.wes.task-execution.task.task.assigned.v1`

**Description:** Published when a task is assigned to a warehouse worker.

**Trigger:** Task assignment via TaskManagementService

**Topic:** `task-events`

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

**Field Descriptions:**
- `task_id`: Unique identifier for the task
- `worker_id`: Identifier of the assigned worker
- `assigned_at`: Timestamp when task was assigned (ISO 8601)
- `priority`: Current task priority
- `zone_id`: Zone where task will be executed

---

### 3. Task Started Event

**Type:** `com.paklog.wes.task-execution.task.task.started.v1`

**Description:** Published when a worker starts executing a task.

**Trigger:** Task start via TaskManagementService

**Topic:** `task-events`

**Schema:**
```json
{
  "task_id": "TASK-12345",
  "worker_id": "WORKER-456",
  "started_at": "2025-10-26T10:02:00Z",
  "estimated_completion_time": "2025-10-26T10:07:00Z"
}
```

**Field Descriptions:**
- `task_id`: Unique identifier for the task
- `worker_id`: Identifier of the worker executing the task
- `started_at`: Timestamp when task execution started (ISO 8601)
- `estimated_completion_time`: Projected completion time (ISO 8601)

---

### 4. Task Completed Event

**Type:** `com.paklog.wes.task-execution.task.task.completed.v1`

**Description:** Published when a task is successfully completed.

**Trigger:** Task completion via TaskManagementService

**Topic:** `task-events`

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

**Field Descriptions:**
- `task_id`: Unique identifier for the task
- `wave_id`: Reference to the originating wave
- `worker_id`: Identifier of the worker who completed the task
- `completed_at`: Timestamp when task was completed (ISO 8601)
- `duration_seconds`: Actual time taken to complete the task
- `items_processed`: Number of items processed during the task

---

## Events Consumed

### Wave Released Event

**Type:** `com.paklog.wms.wave-planning.wave.wave.released.v1`

**Description:** Consumed when wave-planning-service releases a wave for execution. Creates pick tasks for all orders in the wave.

**Source:** `paklog://wave-planning-service`

**Topic:** `wave-events`

**Consumer Group:** `task-execution-service`

**Schema:**
```json
{
  "wave_id": "WAVE-001",
  "wave_number": "W-001",
  "priority": "HIGH",
  "order_ids": ["ORD-001", "ORD-002", "ORD-003"],
  "total_lines": 45,
  "zone_id": "ZONE-A",
  "released_at": "2025-10-26T10:00:00Z"
}
```

**Field Descriptions:**
- `wave_id`: Unique identifier for the wave
- `wave_number`: Human-readable wave number
- `priority`: Wave priority (mapped to internal priority)
- `order_ids`: List of order IDs included in the wave
- `total_lines`: Total number of order lines in the wave
- `zone_id`: Target zone for wave execution
- `released_at`: Timestamp when wave was released (ISO 8601)

**Anti-Corruption Layer:**

The WaveEventConsumer implements an anti-corruption layer that maps external event data to internal domain concepts:

- Priority mapping: External priorities (`URGENT`, `HIGH`, `NORMAL`, `LOW`) are mapped to internal Priority enum
- Task creation: For each order in the wave, a PICK task is created with appropriate context

**Processing:**
1. Receive CloudEvent from wave-events topic
2. Deserialize WaveReleasedContract from event data
3. Map external priority to internal Priority enum
4. Create a PickTask for each order in the wave
5. Tasks are automatically queued for assignment

---

## Kafka Configuration

### Topics

- **task-events**: All task lifecycle events (Created, Assigned, Started, Completed, Failed)
- **wave-events**: Wave planning events consumed from wave-planning-service

### Consumer Groups

- **task-execution-service**: Consumer group for wave events

### Serialization

- **Producer**: `io.cloudevents.kafka.CloudEventSerializer` (structured mode)
- **Consumer**: `io.cloudevents.kafka.CloudEventDeserializer` (structured mode)

### Reliability

- **Producer**: Idempotent with `acks=all` and `retries=3`
- **Consumer**: Manual acknowledgment with concurrency=3

---

## Integration Examples

### Publishing Events (Internal)

Events are automatically published when domain state changes:

```java
// Create and queue a task
WorkTask task = WorkTask.create(...);
task.queue();
taskRepository.save(task);

// Events are published automatically via DomainEventPublisher
domainEventPublisher.publishDomainEvents(task);
```

### Consuming Events (External)

The WaveEventConsumer automatically handles wave events:

```java
@KafkaListener(topics = "wave-events", groupId = "task-execution-service")
public void handleWaveEvent(CloudEvent cloudEvent) {
    if (WaveReleasedContract.EVENT_TYPE.equals(cloudEvent.getType())) {
        // Process wave released event
        // Create tasks for all orders in the wave
    }
}
```

---

## Monitoring and Observability

### Metrics

Key metrics to monitor:

- `task.events.published.total`: Total number of task events published
- `task.events.published.by_type`: Events published grouped by event type
- `wave.events.consumed.total`: Total number of wave events consumed
- `wave.events.processing.duration`: Time taken to process wave events
- `task.creation.from.wave.total`: Tasks created from wave events

### Logs

Event publishing and consumption are logged at INFO level:

```
INFO c.p.t.e.i.e.TaskEventPublisher - Event published: type=com.paklog.wes.task-execution.task.task.created.v1, key=TASK-123, topic=task-events
INFO c.p.t.e.i.e.WaveEventConsumer - Wave released: waveId=WAVE-001, orders=3, priority=HIGH
INFO c.p.t.e.i.e.WaveEventConsumer - Created pick task for order: orderId=ORD-001, waveId=WAVE-001, priority=HIGH
```

---

## Versioning Strategy

Events use semantic versioning in the type field:

- **Breaking changes**: Increment major version (e.g., `v1` → `v2`)
- **Backward-compatible additions**: Increment minor version (e.g., `v1.0` → `v1.1`)
- **Bug fixes**: Increment patch version (e.g., `v1.0.0` → `v1.0.1`)

Current version: **v1** (all events)

### Evolution Guidelines

1. **Never remove fields** from existing event schemas
2. **Add optional fields** for backward compatibility
3. **Create new event types** for significant changes
4. **Maintain old versions** for at least 6 months during migration

---

## Error Handling

### Producer Errors

If event publishing fails:
- Error is logged but does not fail the transaction
- Domain state is still persisted
- Consider implementing dead letter queue for failed events

### Consumer Errors

If event processing fails:
- Error is logged with full context
- Kafka retry mechanism handles transient failures
- Consider implementing error topic for permanent failures

---

## Security

### Authentication

- Kafka authentication via SASL/SCRAM or mTLS (configured via bootstrap servers)
- API keys for inter-service communication

### Authorization

- Topic-level ACLs configured in Kafka
- task-execution-service has:
  - WRITE access to `task-events`
  - READ access to `wave-events`

### Data Protection

- Events contain operational data only (no PII)
- Sensitive fields should be encrypted at application level if needed
- TLS encryption in transit (configured via Kafka)

---

## References

- [CloudEvents Specification v1.0](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/spec.md)
- [CloudEvents Kafka Protocol Binding](https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/bindings/kafka-protocol-binding.md)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
