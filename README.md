# Task Execution Service

Warehouse task orchestration service with intelligent assignment, Redis-based queuing, and priority calculation for optimal labor productivity.

## Overview

The Task Execution Service manages the complete lifecycle of warehouse tasks across all operational areas including picking, packing, putaway, replenishment, and cycle counting. This bounded context receives task requests from upstream services, calculates dynamic priorities, queues tasks efficiently using Redis, and assigns work to mobile workers based on skill, location proximity, and current workload. The service integrates with mobile applications to provide real-time task assignment and tracking.

## Domain-Driven Design

### Bounded Context
**Task Execution & Assignment** - Manages warehouse task lifecycle from creation through completion with intelligent worker assignment and queue management.

### Core Domain Model

#### Aggregates
- **WorkTask** - Root aggregate representing a unit of warehouse work

#### Entities
- **TaskContext** - Polymorphic task-specific data (PickTaskContext, PackTaskContext, etc.)

#### Value Objects
- **TaskType** - Task category enumeration (PICK, PACK, PUTAWAY, REPLENISH, COUNT, MOVE, SHIP)
- **TaskStatus** - Task lifecycle status (PENDING, QUEUED, ASSIGNED, ACCEPTED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED)
- **Priority** - Task priority level (CRITICAL, HIGH, NORMAL, LOW)
- **Location** - Physical warehouse location reference

#### Domain Events
- **TaskCreatedEvent** - New task created
- **TaskAssignedEvent** - Task assigned to worker
- **TaskAcceptedEvent** - Worker accepted task
- **TaskStartedEvent** - Task execution started
- **TaskCompletedEvent** - Task successfully completed
- **TaskFailedEvent** - Task execution failed
- **TaskCancelledEvent** - Task cancelled

### Ubiquitous Language
- **Work Task**: Unit of warehouse work to be executed by an operator
- **Task Context**: Type-specific task data and parameters
- **Task Queue**: Prioritized collection of pending tasks
- **Task Assignment**: Matching tasks to available workers
- **Priority Score**: Calculated value for task ordering in queue
- **Complexity Score**: Task difficulty rating affecting duration estimates
- **Worker Proximity**: Distance between worker and task location
- **Task Pool**: Collection of queued tasks available for assignment

## Architecture & Patterns

### Hexagonal Architecture (Ports and Adapters)

```
src/main/java/com/paklog/wes/task/
├── domain/                           # Core business logic
│   ├── aggregate/                   # Aggregates
│   │   └── WorkTask.java            # Task aggregate root
│   ├── entity/                      # Entities
│   │   ├── TaskContext.java         # Base task context
│   │   ├── PickTaskContext.java     # Pick-specific context
│   │   ├── PackTaskContext.java     # Pack-specific context
│   │   ├── PutawayTaskContext.java  # Putaway-specific context
│   │   ├── ReplenishTaskContext.java # Replenish-specific context
│   │   ├── CountTaskContext.java    # Count-specific context
│   │   ├── MoveTaskContext.java     # Move-specific context
│   │   └── ShipTaskContext.java     # Ship-specific context
│   ├── valueobject/                 # Value objects
│   │   ├── TaskType.java
│   │   ├── TaskStatus.java
│   │   ├── Priority.java
│   │   └── Location.java
│   ├── repository/                  # Repository interfaces
│   │   └── WorkTaskRepository.java
│   ├── service/                     # Domain services
│   │   ├── TaskPriorityCalculator.java
│   │   └── TaskAssignmentStrategy.java
│   └── event/                       # Domain events
├── application/                      # Use cases & orchestration
│   ├── service/                     # Application services
│   │   ├── TaskManagementService.java
│   │   └── TaskAssignmentService.java
│   ├── command/                     # Commands
│   └── query/                       # Queries
└── adapter/                          # External adapters
    ├── rest/                        # REST controllers
    ├── mobile/                      # Mobile API
    ├── persistence/                 # MongoDB repositories
    ├── queue/                       # Redis queue implementation
    └── events/                      # Event publishers/consumers
```

### Design Patterns & Principles
- **Hexagonal Architecture** - Clean separation of domain and infrastructure
- **Domain-Driven Design** - Rich domain model with task lifecycle management
- **Strategy Pattern** - Pluggable task assignment algorithms
- **Priority Queue Pattern** - Redis-based task prioritization
- **Polymorphic Context Pattern** - Type-specific task data handling
- **Event-Driven Architecture** - Asynchronous task event publishing
- **Repository Pattern** - Data access abstraction
- **SOLID Principles** - Maintainable and extensible code

## Technology Stack

### Core Framework
- **Java 21** - Programming language
- **Spring Boot 3.3.3** - Application framework
- **Maven** - Build and dependency management

### Data & Persistence
- **MongoDB** - Document database for task storage
- **Spring Data MongoDB** - Data access layer
- **Redis** - Task queue and caching
- **Spring Data Redis** - Redis integration

### Messaging & Events
- **Apache Kafka** - Event streaming platform
- **Spring Kafka** - Kafka integration
- **CloudEvents 2.5.0** - Standardized event format

### API & Documentation
- **Spring Web MVC** - REST API framework
- **Bean Validation** - Input validation
- **OpenAPI/Swagger** - API documentation

### Observability
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer** - Metrics collection
- **Micrometer Tracing** - Distributed tracing
- **Loki Logback Appender** - Log aggregation

### Testing
- **JUnit 5** - Unit testing framework
- **Testcontainers** - Integration testing
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Local development environment

## Standards Applied

### Architectural Standards
- ✅ Hexagonal Architecture (Ports and Adapters)
- ✅ Domain-Driven Design tactical patterns
- ✅ Event-Driven Architecture
- ✅ Microservices architecture
- ✅ RESTful API design
- ✅ Priority queue pattern with Redis

### Code Quality Standards
- ✅ SOLID principles
- ✅ Clean Code practices
- ✅ Comprehensive unit and integration testing
- ✅ Domain-driven design patterns
- ✅ Immutable value objects
- ✅ Rich domain models with business logic

### Event & Integration Standards
- ✅ CloudEvents specification v1.0
- ✅ Event-driven task coordination
- ✅ At-least-once delivery semantics
- ✅ Event versioning strategy
- ✅ Idempotent event handling

### Observability Standards
- ✅ Structured logging (JSON)
- ✅ Distributed tracing
- ✅ Health check endpoints
- ✅ Prometheus metrics
- ✅ Correlation ID propagation

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- Redis 7.0+

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/paklog/task-execution-service.git
   cd task-execution-service
   ```

2. **Start infrastructure services**
   ```bash
   docker-compose up -d mongodb redis kafka
   ```

3. **Build and run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Verify the service is running**
   ```bash
   curl http://localhost:8082/actuator/health
   ```

### Using Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f task-execution-service

# Stop all services
docker-compose down
```

## API Documentation

Once running, access the interactive API documentation:
- **Swagger UI**: http://localhost:8082/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8082/v3/api-docs

### Key Endpoints

- `POST /tasks` - Create new task
- `GET /tasks/{taskId}` - Get task by ID
- `POST /tasks/{taskId}/queue` - Queue task for assignment
- `POST /tasks/{taskId}/assign` - Assign task to worker
- `POST /tasks/{taskId}/accept` - Accept assigned task
- `POST /tasks/{taskId}/start` - Start task execution
- `POST /tasks/{taskId}/complete` - Complete task
- `POST /tasks/{taskId}/fail` - Mark task as failed
- `POST /tasks/{taskId}/cancel` - Cancel task
- `GET /tasks/worker/{workerId}` - Get tasks assigned to worker
- `GET /tasks/mobile/next` - Get next task recommendation for mobile worker
- `GET /tasks/queue` - View current task queue

## Task Management Features

### Priority Calculation Algorithm

The service implements a sophisticated priority calculation algorithm:

```java
Priority Score = Base Priority Value
                 - (Task Age in Minutes)
                 - (Overdue Penalty: 10,000 points)
                 + (Complexity Score × 100)
```

- **Lower score = higher priority in queue**
- **Dynamic aging**: Tasks become higher priority over time
- **Overdue boost**: Past-deadline tasks jump to front of queue
- **Complexity consideration**: More complex tasks get appropriate weighting

### Task Assignment Strategy

Intelligent task-to-worker assignment based on:
- **Skill matching**: Worker qualifications vs task requirements
- **Location proximity**: Distance between worker and task location
- **Current workload**: Worker's active task count
- **Task type affinity**: Worker preference and experience
- **Equipment availability**: Required equipment accessibility

### Redis Queue Management

- **Multi-zone queues**: Separate queues per warehouse zone
- **Priority-based ordering**: Tasks ordered by calculated priority score
- **TTL management**: Automatic cleanup of stale tasks
- **Atomic operations**: Thread-safe queue operations
- **Queue metrics**: Real-time queue depth and throughput monitoring

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run tests with coverage
mvn clean verify jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Configuration

Key configuration properties:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/task_execution
    redis:
      host: localhost
      port: 6379
  kafka:
    bootstrap-servers: localhost:9092

task-execution:
  queue:
    redis-enabled: true
    priority-calculation:
      age-weight: 1.0
      complexity-weight: 100.0
      overdue-penalty: 10000.0
  assignment:
    strategy: proximity-based
    max-distance-meters: 500
    skill-matching: strict
```

## Event Integration

### Published Events
- `com.paklog.wes.task.created.v1`
- `com.paklog.wes.task.assigned.v1`
- `com.paklog.wes.task.accepted.v1`
- `com.paklog.wes.task.started.v1`
- `com.paklog.wes.task.completed.v1`
- `com.paklog.wes.task.failed.v1`
- `com.paklog.wes.task.cancelled.v1`

### Consumed Events
- `com.paklog.fulfillment.order.released.v1` - Create pick tasks
- `com.paklog.wes.pick.session.completed.v1` - Create pack tasks
- `com.paklog.warehouse.receiving.completed.v1` - Create putaway tasks
- `com.paklog.inventory.replenishment.required.v1` - Create replenish tasks
- `com.paklog.inventory.cycle.count.scheduled.v1` - Create count tasks

### Event Format
All events follow the CloudEvents specification v1.0 and are published asynchronously via Kafka.

## Task Lifecycle

```
PENDING → QUEUED → ASSIGNED → ACCEPTED → IN_PROGRESS → COMPLETED
    ↓        ↓         ↓
CANCELLED  CANCELLED  FAILED
```

### State Transitions
- **PENDING → QUEUED**: Task added to priority queue
- **QUEUED → ASSIGNED**: Task assigned to worker
- **ASSIGNED → ACCEPTED**: Worker accepts assignment
- **ASSIGNED → QUEUED**: Worker rejects (task returns to queue)
- **ACCEPTED → IN_PROGRESS**: Worker starts execution
- **IN_PROGRESS → COMPLETED**: Successful completion
- **IN_PROGRESS → FAILED**: Execution failure
- **Any → CANCELLED**: Task cancelled (except terminal states)

## Monitoring

- **Health**: http://localhost:8082/actuator/health
- **Metrics**: http://localhost:8082/actuator/metrics
- **Prometheus**: http://localhost:8082/actuator/prometheus
- **Info**: http://localhost:8082/actuator/info

### Key Metrics
- `tasks.created.total` - Total tasks created
- `tasks.completed.total` - Total tasks completed
- `tasks.queue.depth` - Current queue depth by zone
- `tasks.assignment.duration` - Time to assign tasks
- `tasks.execution.duration` - Task execution time
- `tasks.priority.score` - Average priority scores

## Contributing

1. Follow hexagonal architecture principles
2. Implement domain logic in domain layer
3. Use strategy pattern for assignment algorithms
4. Maintain task lifecycle state transitions
5. Calculate priorities dynamically
6. Write comprehensive tests including domain model tests
7. Document domain concepts using ubiquitous language
8. Follow existing code style and conventions

## License

Copyright © 2024 Paklog. All rights reserved.
