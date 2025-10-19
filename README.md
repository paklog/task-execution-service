# Task Execution Service (WES)

Unified task management and execution orchestration service.

## Responsibilities

- Unified task model for all work types (PICK, PACK, PUTAWAY, COUNT, REPLENISH)
- Task generation from waves, orders, and events
- Task assignment and routing to associates
- Task queue management and prioritization
- Real-time task status tracking
- Mobile app API for task execution
- Labor productivity tracking

## Architecture

```
domain/
├── aggregate/      # WorkTask, TaskAssignment
├── entity/         # TaskQueue, Associate
├── valueobject/    # TaskType, TaskStatus, Priority
├── service/        # TaskOrchestrationService, TaskAssignmentService
├── repository/     # WorkTaskRepository
├── event/          # TaskCreatedEvent, TaskAssignedEvent
└── factory/        # PickTaskFactory, PackTaskFactory

application/
├── command/        # CreateTaskCommand, AssignTaskCommand
├── query/          # GetAssignedTasksQuery, GetTaskQueueQuery
└── handler/        # WaveReleasedHandler, InventoryEventHandler

adapter/
├── rest/           # Task management controllers
├── mobile/         # Mobile API controllers
└── persistence/    # MongoDB repositories

infrastructure/
├── config/         # Spring configurations
├── messaging/      # Kafka publishers/consumers
├── events/         # Event publishing infrastructure
└── queue/          # Redis queue management
```

## Tech Stack

- Java 21
- Spring Boot 3.2.0
- MongoDB (task persistence)
- Redis (queue management)
- Apache Kafka (event-driven integration)
- WebSocket (real-time updates)
- CloudEvents
- OpenAPI/Swagger

## Running the Service

```bash
mvn spring-boot:run
```

## API Documentation

Available at: http://localhost:8082/swagger-ui.html

## Events Published

- `TaskCreatedEvent` - When a task is created
- `TaskAssignedEvent` - When a task is assigned to a worker
- `TaskStartedEvent` - When work begins on a task
- `TaskCompletedEvent` - When a task is completed

## Events Consumed

- `WaveReleasedEvent` - From Wave Planning Service
- `InventoryMovedEvent` - From Inventory Service
- `OrderPackedEvent` - From Pack & Ship Service
