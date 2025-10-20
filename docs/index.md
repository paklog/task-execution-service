---
layout: default
title: Home
---

# Task Execution Service Documentation

Warehouse task assignment and execution management service with intelligent priority calculation and Redis-based queue management.

## Overview

The Task Execution Service manages the complete lifecycle of warehouse work tasks including picking, putaway, replenishment, cycle counting, and material moves. It implements sophisticated task priority calculation, intelligent operator assignment, and Redis-based queue management for high-performance task distribution. The service uses Domain-Driven Design with event-driven architecture for real-time warehouse operations.

## Quick Links

### Getting Started
- [README](README.md) - Quick start guide and overview
- [Architecture Overview](architecture.md) - System architecture description

### Architecture & Design
- [Domain Model](DOMAIN-MODEL.md) - Complete domain model with class diagrams
- [Sequence Diagrams](SEQUENCE-DIAGRAMS.md) - Process flows and interactions
- [OpenAPI Specification](openapi.yaml) - REST API documentation
- [AsyncAPI Specification](asyncapi.yaml) - Event documentation

## Technology Stack

- **Java 21** - Programming language
- **Spring Boot 3.2** - Application framework
- **MongoDB** - Document database for task persistence
- **Redis** - Priority queue and caching
- **Apache Kafka** - Event streaming platform
- **CloudEvents 2.5.0** - Event standard
- **Maven** - Build tool

## Key Features

- **Multi-Type Task Management** - Picking, putaway, replenishment, cycle count, move
- **Intelligent Priority Calculation** - Multi-factor dynamic priority scoring
- **Redis Priority Queues** - High-performance task queuing
- **Smart Operator Assignment** - Zone-based, skill-based, load-balanced allocation
- **Real-time Task Tracking** - Live task progress monitoring
- **Task Batching** - Efficient batch processing for performance

## Domain Model

### Aggregates
- **WorkTask** - Complete task lifecycle management
- **Operator** - Warehouse operator management

### Entities
- **TaskDetails** - Polymorphic task-specific details (Pick, Putaway, Replenish, Count, Move)
- **TaskItem** - Items associated with tasks

### Value Objects
- **Location** - Physical location data
- **OperatorMetrics** - Operator performance metrics
- **PriorityContext** - Priority calculation context

### Task Lifecycle

```
PENDING -> ASSIGNED -> IN_PROGRESS -> COMPLETED
                 \-> CANCELLED
                       \-> PAUSED -> IN_PROGRESS
                              \-> FAILED
```

## Domain Events

### Published Events
- **TaskCreated** - New task created
- **TaskAssigned** - Task assigned to operator
- **TaskStarted** - Task execution started
- **TaskCompleted** - Task execution completed
- **TaskCancelled** - Task cancelled
- **TaskFailed** - Task execution failed
- **TaskPriorityChanged** - Task priority updated
- **TaskPaused** - Task paused
- **TaskResumed** - Task resumed
- **OperatorStatusChanged** - Operator status updated

### Consumed Events
- **WaveReleased** - Create picking tasks from wave
- **ReceiptCompleted** - Create putaway tasks
- **InventoryBelowMin** - Create replenishment tasks

## Architecture Patterns

- **Hexagonal Architecture** - Ports and adapters for clean separation
- **Domain-Driven Design** - Rich domain model with business logic
- **Event-Driven Architecture** - Asynchronous integration via events
- **Strategy Pattern** - Multiple task allocation strategies
- **Priority Queue Pattern** - Redis sorted sets for task queuing

## API Endpoints

### Task Management
- `POST /tasks` - Create new task
- `GET /tasks/{taskId}` - Get task details
- `PUT /tasks/{taskId}/assign` - Assign task to operator
- `POST /tasks/{taskId}/start` - Start task execution
- `POST /tasks/{taskId}/complete` - Complete task
- `POST /tasks/{taskId}/cancel` - Cancel task
- `GET /tasks` - List tasks with filtering

### Task Operations
- `POST /tasks/{taskId}/pause` - Pause task
- `POST /tasks/{taskId}/resume` - Resume task
- `PUT /tasks/{taskId}/priority` - Update task priority
- `GET /tasks/queue/status` - Get queue metrics

### Operator Management
- `GET /operators` - List operators
- `GET /operators/{operatorId}` - Get operator details
- `POST /operators/{operatorId}/online` - Operator goes online
- `POST /operators/{operatorId}/offline` - Operator goes offline
- `GET /operators/{operatorId}/next-task` - Get next task for operator

## Task Priority Calculation

The service uses a sophisticated multi-factor priority calculation:

### Priority Factors
- **SLA Urgency** - 35% weight - Highest priority
- **Carrier Cutoffs** - 30% weight - Shipping deadline proximity
- **Customer Tier** - 20% weight - Customer priority level
- **Zone Efficiency** - 10% weight - Operator zone proximity
- **Age-based Escalation** - 5% weight - Task aging priority

### Priority Modifiers by Task Type
- **PUTAWAY**: 1.1x (free up receiving)
- **PICKING**: 1.0x (normal priority)
- **REPLENISHMENT**: 0.8x (lower priority)
- **MOVE**: 0.7x (lower priority)
- **CYCLE_COUNT**: 0.6x (lowest priority)

## Task Allocation Strategies

### Zone-Based Allocation
Assigns tasks to operators in the same zone to minimize travel.

### Skill-Based Allocation
Matches task requirements with operator capabilities.

### Load-Balanced Allocation
Distributes tasks evenly across available operators.

## Queue Management

### Redis Priority Queues
- O(log n) enqueue/dequeue performance
- Priority-based ordering (lower score = higher priority)
- Zone and type-based queue segmentation
- Configurable task expiration

### Queue Operations
- `enqueue()` - Add task to priority queue
- `dequeue()` - Get highest priority task
- `remove()` - Cancel queued task
- `getQueueStatus()` - Queue metrics and depth
- `peek()` - View next task without removal

## Integration Points

### Consumes Events From
- Wave Planning (wave released)
- Receiving (receipt completed)
- Inventory (inventory below minimum)
- Pick Execution (pick completed)

### Publishes Events To
- Pick Execution (picking task assigned)
- Physical Tracking (task movements)
- Workload Planning (task metrics)

## Performance Considerations

- Priority calculation cached for 5 minutes
- Queue operations use Redis sorted sets for O(log n) performance
- Batch processing for multiple task updates
- Asynchronous event publishing
- Connection pooling for Redis operations
- MongoDB indexes on status, operator, warehouse, zone fields

## Business Rules

1. **Task Status Transitions**
   - Tasks must follow valid state transitions
   - Completed tasks cannot be modified
   - Failed tasks can be retried or cancelled

2. **Assignment Rules**
   - Operator must have required capabilities
   - Prefer operators in same zone
   - Balance workload across operators
   - Respect operator availability

3. **Priority Rules**
   - Priority recalculated on status changes
   - Dynamic adjustment based on context
   - Automatic escalation for aging tasks
   - SLA urgency takes precedence

4. **Queue Management**
   - Lower score = higher priority in queue
   - Tasks expire after configurable time
   - Failed tasks can be requeued
   - Batch dequeue for efficiency

## Task Types

### Picking Tasks
- Order-based picking operations
- Wave-associated picks
- Carrier and SLA tracking

### Putaway Tasks
- Receipt-based putaway
- Strategic location assignment
- Container management

### Replenishment Tasks
- Min/max quantity triggers
- Source and destination tracking
- Priority-based execution

### Cycle Count Tasks
- Location-based counts
- Variance investigation
- Count method specification

### Move Tasks
- Material relocation
- Reason tracking
- Multi-item moves

## Getting Started

1. Review the [README](README.md) for quick start instructions
2. Understand the [Architecture](architecture.md) and design patterns
3. Explore the [Domain Model](DOMAIN-MODEL.md) to understand business concepts
4. Study the [Sequence Diagrams](SEQUENCE-DIAGRAMS.md) for process flows
5. Reference the [OpenAPI](openapi.yaml) and [AsyncAPI](asyncapi.yaml) specifications

## Configuration

Key configuration properties:
- `task.priority.sla-weight` - SLA urgency weight (default: 0.35)
- `task.priority.cutoff-weight` - Carrier cutoff weight (default: 0.30)
- `task.priority.customer-weight` - Customer tier weight (default: 0.20)
- `task.priority.cache-ttl` - Priority cache TTL (default: 5m)
- `task.queue.expiration` - Task expiration time (default: 24h)
- `redis.pool.max-connections` - Redis connection pool size

## Contributing

For contribution guidelines, please refer to the main README in the project root.

## Support

- **GitHub Issues**: Report bugs or request features
- **Documentation**: Browse the guides in the navigation menu
- **Service Owner**: WMS Team
- **Slack**: #wms-task-execution
