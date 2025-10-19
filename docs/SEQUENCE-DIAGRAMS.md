# Task Execution Service - Sequence Diagrams

## 1. Task Creation and Priority Calculation Flow

### Create Pick Task from Wave Release

```mermaid
sequenceDiagram
    autonumber
    participant WaveService
    participant Kafka
    participant TaskEventHandler
    participant TaskService
    participant TaskPriorityCalculator
    participant TaskQueueManager
    participant TaskRepository
    participant Redis

    WaveService->>Kafka: wave.released
    Note over WaveService: Wave with order details

    Kafka->>TaskEventHandler: consume(WaveReleased)

    TaskEventHandler->>TaskService: createPickTasks(wave)

    loop For each order in wave
        TaskService->>TaskService: createPickTask(order)

        TaskService->>TaskPriorityCalculator: calculatePriority(task)

        TaskPriorityCalculator->>TaskPriorityCalculator: calculateSLAScore()
        Note over TaskPriorityCalculator: 35% weight

        TaskPriorityCalculator->>TaskPriorityCalculator: calculateCutoffScore()
        Note over TaskPriorityCalculator: 30% weight

        TaskPriorityCalculator->>TaskPriorityCalculator: calculateCustomerScore()
        Note over TaskPriorityCalculator: 20% weight

        TaskPriorityCalculator->>TaskPriorityCalculator: calculateZoneScore()
        Note over TaskPriorityCalculator: 10% weight

        TaskPriorityCalculator->>TaskPriorityCalculator: calculateAgeScore()
        Note over TaskPriorityCalculator: 5% weight

        TaskPriorityCalculator->>TaskPriorityCalculator: applyTaskTypeModifier()
        TaskPriorityCalculator-->>TaskService: priorityScore

        TaskService->>TaskService: setPriorityScore(priorityScore)

        TaskService->>TaskRepository: save(task)
        TaskRepository-->>TaskService: Task (saved)

        TaskService->>TaskQueueManager: enqueue(task)
        TaskQueueManager->>Redis: ZADD queue:WH:ZONE:PICK score taskId
        Redis-->>TaskQueueManager: Added

        TaskService->>Kafka: task.created
    end

    TaskService-->>TaskEventHandler: TasksCreated
    TaskEventHandler-->>WaveService: Acknowledgment
```

### Create Putaway Task from Receipt

```mermaid
sequenceDiagram
    autonumber
    participant ReceivingService
    participant TaskController
    participant TaskService
    participant TaskPriorityCalculator
    participant LocationService
    participant TaskRepository
    participant TaskQueueManager

    ReceivingService->>TaskController: POST /tasks/putaway
    Note over ReceivingService: Receipt details

    TaskController->>TaskService: createPutawayTask(request)

    TaskService->>LocationService: findPutawayLocation(item)
    LocationService->>LocationService: applyPutawayStrategy()
    LocationService-->>TaskService: Location

    TaskService->>TaskService: createTask(PUTAWAY)
    TaskService->>TaskService: setPutawayDetails()

    TaskService->>TaskPriorityCalculator: calculatePriority(task)
    Note over TaskPriorityCalculator: PUTAWAY gets 1.1x modifier

    TaskPriorityCalculator-->>TaskService: priorityScore

    TaskService->>TaskRepository: save(task)
    TaskRepository-->>TaskService: Task (saved)

    TaskService->>TaskQueueManager: enqueue(task)
    TaskQueueManager-->>TaskService: Enqueued

    TaskService->>Kafka: task.created (PUTAWAY)

    TaskService-->>TaskController: Task created
    TaskController-->>ReceivingService: 201 Created (taskId)
```

## 2. Task Assignment and Dequeue Flow

### Operator Requests Next Task

```mermaid
sequenceDiagram
    autonumber
    participant MobileApp
    participant TaskController
    participant TaskAssignmentService
    participant OperatorService
    participant TaskQueueManager
    participant TaskService
    participant TaskRepository
    participant Redis

    MobileApp->>TaskController: GET /tasks/next
    Note over MobileApp: Operator ID in header

    TaskController->>TaskAssignmentService: getNextTask(operatorId)

    TaskAssignmentService->>OperatorService: getOperator(operatorId)
    OperatorService-->>TaskAssignmentService: Operator

    TaskAssignmentService->>TaskAssignmentService: validateOperatorAvailable()

    TaskAssignmentService->>TaskQueueManager: dequeue(operatorId, warehouse, zone, capabilities)

    TaskQueueManager->>TaskQueueManager: getEligibleQueues()
    Note over TaskQueueManager: Based on capabilities

    loop For each eligible queue
        TaskQueueManager->>Redis: ZRANGE queue 0 0
        Redis-->>TaskQueueManager: taskId (if exists)

        alt Task found
            TaskQueueManager->>Redis: ZREM queue taskId
            Redis-->>TaskQueueManager: Removed
            break Task dequeued
        end
    end

    TaskQueueManager-->>TaskAssignmentService: taskId

    TaskAssignmentService->>TaskService: assignTask(taskId, operatorId)

    TaskService->>TaskRepository: findById(taskId)
    TaskRepository-->>TaskService: Task

    TaskService->>TaskService: updateStatus(ASSIGNED)
    TaskService->>TaskService: setOperator(operatorId)

    TaskService->>TaskRepository: save(task)
    TaskRepository-->>TaskService: Task (assigned)

    TaskService->>Kafka: task.assigned

    TaskService-->>TaskAssignmentService: AssignedTask

    TaskAssignmentService->>OperatorService: updateOperatorStatus(BUSY)

    TaskAssignmentService-->>TaskController: Task details
    TaskController-->>MobileApp: 200 OK (task)
```

### Batch Task Assignment

```mermaid
sequenceDiagram
    autonumber
    participant Scheduler
    participant TaskAssignmentService
    participant OperatorService
    participant TaskQueueManager
    participant LoadBalancer
    participant TaskService

    Scheduler->>TaskAssignmentService: runBatchAssignment()
    Note over Scheduler: Runs every 30 seconds

    TaskAssignmentService->>OperatorService: findAvailableOperators()
    OperatorService-->>TaskAssignmentService: List<Operator>

    TaskAssignmentService->>TaskQueueManager: getPendingTaskCounts()
    TaskQueueManager-->>TaskAssignmentService: QueueMetrics

    TaskAssignmentService->>LoadBalancer: calculateOptimalAssignment(operators, tasks)

    LoadBalancer->>LoadBalancer: groupTasksByZone()
    LoadBalancer->>LoadBalancer: matchOperatorsToZones()
    LoadBalancer->>LoadBalancer: balanceWorkload()

    LoadBalancer-->>TaskAssignmentService: AssignmentPlan

    loop For each assignment
        TaskAssignmentService->>TaskQueueManager: dequeue(specific task)
        TaskQueueManager-->>TaskAssignmentService: taskId

        TaskAssignmentService->>TaskService: assignTask(taskId, operatorId)
        TaskService-->>TaskAssignmentService: Assigned

        TaskAssignmentService->>Kafka: task.assigned
    end

    TaskAssignmentService-->>Scheduler: BatchAssignmentResult
```

## 3. Task Execution Flow

### Start Task Execution

```mermaid
sequenceDiagram
    autonumber
    participant MobileApp
    participant TaskController
    participant TaskService
    participant TaskRepository
    participant LocationService
    participant EventPublisher

    MobileApp->>TaskController: PUT /tasks/{id}/start
    Note over MobileApp: At pick location

    TaskController->>TaskService: startTask(taskId, operatorLocation)

    TaskService->>TaskRepository: findById(taskId)
    TaskRepository-->>TaskService: Task

    TaskService->>TaskService: validateCanStart()
    Note over TaskService: Must be ASSIGNED

    TaskService->>LocationService: validateOperatorAtLocation(operator, task.sourceLocation)
    LocationService-->>TaskService: ValidationResult

    alt At correct location
        TaskService->>TaskService: updateStatus(IN_PROGRESS)
        TaskService->>TaskService: setStartTime()

        TaskService->>TaskRepository: save(task)
        TaskRepository-->>TaskService: Task (started)

        TaskService->>EventPublisher: publish(TaskStarted)
        EventPublisher->>Kafka: task.started

        TaskService-->>TaskController: TaskStarted
        TaskController-->>MobileApp: 200 OK
    else Wrong location
        TaskService-->>TaskController: LocationError
        TaskController-->>MobileApp: 400 Bad Request
    end
```

### Complete Task

```mermaid
sequenceDiagram
    autonumber
    participant MobileApp
    participant TaskController
    participant TaskService
    participant ValidationService
    participant TaskRepository
    participant OperatorService
    participant MetricsService
    participant EventPublisher

    MobileApp->>TaskController: PUT /tasks/{id}/complete
    Note over MobileApp: Completion details

    TaskController->>TaskService: completeTask(taskId, completionData)

    TaskService->>TaskRepository: findById(taskId)
    TaskRepository-->>TaskService: Task

    TaskService->>ValidationService: validateCompletion(task, data)
    ValidationService->>ValidationService: checkQuantities()
    ValidationService->>ValidationService: checkLocations()
    ValidationService-->>TaskService: ValidationResult

    alt Valid completion
        TaskService->>TaskService: updateStatus(COMPLETED)
        TaskService->>TaskService: setCompletionTime()
        TaskService->>TaskService: calculateDuration()

        TaskService->>TaskRepository: save(task)
        TaskRepository-->>TaskService: Task (completed)

        TaskService->>OperatorService: updateOperatorStatus(AVAILABLE)
        OperatorService-->>TaskService: Updated

        TaskService->>MetricsService: recordTaskMetrics(task)
        MetricsService->>MetricsService: updateOperatorMetrics()
        MetricsService->>MetricsService: updateZoneMetrics()

        TaskService->>EventPublisher: publish(TaskCompleted)
        EventPublisher->>Kafka: task.completed

        TaskService-->>TaskController: TaskCompleted
        TaskController-->>MobileApp: 200 OK

        MobileApp->>TaskController: GET /tasks/next
        Note over MobileApp: Auto-request next task
    else Invalid completion
        TaskService-->>TaskController: ValidationError
        TaskController-->>MobileApp: 400 Bad Request
    end
```

## 4. Priority Adjustment Flow

### Dynamic Priority Recalculation

```mermaid
sequenceDiagram
    autonumber
    participant Scheduler
    participant PriorityAdjustmentService
    participant TaskRepository
    participant TaskPriorityCalculator
    participant SystemMetricsService
    participant TaskQueueManager
    participant Redis

    Scheduler->>PriorityAdjustmentService: runPriorityAdjustment()
    Note over Scheduler: Every 5 minutes

    PriorityAdjustmentService->>TaskRepository: findPendingTasks()
    TaskRepository-->>PriorityAdjustmentService: List<Task>

    PriorityAdjustmentService->>SystemMetricsService: getSystemLoad()
    SystemMetricsService-->>PriorityAdjustmentService: SystemLoadMetrics

    loop For each task
        PriorityAdjustmentService->>TaskPriorityCalculator: calculateDynamicPriority(task, context)

        TaskPriorityCalculator->>TaskPriorityCalculator: checkSLAProximity()
        TaskPriorityCalculator->>TaskPriorityCalculator: checkCarrierCutoff()
        TaskPriorityCalculator->>TaskPriorityCalculator: applyAgeBoost()
        TaskPriorityCalculator->>TaskPriorityCalculator: applyContextFactors()

        TaskPriorityCalculator-->>PriorityAdjustmentService: newPriority

        alt Priority changed significantly
            PriorityAdjustmentService->>TaskRepository: updatePriority(taskId, newPriority)

            PriorityAdjustmentService->>TaskQueueManager: remove(task)
            TaskQueueManager->>Redis: ZREM queue taskId

            PriorityAdjustmentService->>TaskQueueManager: enqueue(task)
            TaskQueueManager->>Redis: ZADD queue newScore taskId

            PriorityAdjustmentService->>Kafka: task.priority.changed
        end
    end

    PriorityAdjustmentService-->>Scheduler: AdjustmentComplete
```

### Manual Priority Override

```mermaid
sequenceDiagram
    autonumber
    participant Supervisor
    participant TaskController
    participant TaskService
    participant TaskPriorityCalculator
    participant TaskRepository
    participant TaskQueueManager
    participant AuditService

    Supervisor->>TaskController: PUT /tasks/{id}/priority
    Note over Supervisor: New priority with reason

    TaskController->>TaskService: overridePriority(taskId, priority, reason)

    TaskService->>TaskRepository: findById(taskId)
    TaskRepository-->>TaskService: Task

    TaskService->>TaskService: validateCanOverride()
    Note over TaskService: Check permissions

    TaskService->>TaskPriorityCalculator: calculateOverrideScore(priority)
    TaskPriorityCalculator-->>TaskService: priorityScore

    TaskService->>TaskService: updatePriority(priorityScore)
    TaskService->>TaskService: setOverrideReason(reason)

    TaskService->>TaskRepository: save(task)
    TaskRepository-->>TaskService: Task (updated)

    TaskService->>TaskQueueManager: requeue(task)
    TaskQueueManager->>TaskQueueManager: remove(task)
    TaskQueueManager->>TaskQueueManager: enqueue(task)

    TaskService->>AuditService: logPriorityOverride(task, user, reason)

    TaskService->>Kafka: task.priority.overridden

    TaskService-->>TaskController: PriorityUpdated
    TaskController-->>Supervisor: 200 OK
```

## 5. Task Exception Handling

### Task Failure and Recovery

```mermaid
sequenceDiagram
    autonumber
    participant MobileApp
    participant TaskController
    participant TaskService
    participant ExceptionHandler
    participant TaskRepository
    participant RetryManager
    participant NotificationService

    MobileApp->>TaskController: PUT /tasks/{id}/fail
    Note over MobileApp: Failure reason

    TaskController->>TaskService: failTask(taskId, reason)

    TaskService->>TaskRepository: findById(taskId)
    TaskRepository-->>TaskService: Task

    TaskService->>ExceptionHandler: handleTaskFailure(task, reason)

    ExceptionHandler->>ExceptionHandler: classifyFailure()
    Note over ExceptionHandler: Determine if retryable

    alt Retryable failure
        ExceptionHandler->>RetryManager: scheduleRetry(task)
        RetryManager->>RetryManager: calculateBackoff()
        RetryManager->>RetryManager: incrementRetryCount()

        RetryManager->>TaskService: updateForRetry(task)
        TaskService->>TaskService: updateStatus(PENDING)
        TaskService->>TaskService: incrementRetryCount()

        TaskService->>TaskRepository: save(task)

        TaskService->>TaskQueueManager: enqueue(task)
        Note over TaskQueueManager: Re-queue with adjusted priority

        ExceptionHandler->>NotificationService: notifyRetry(task)
    else Non-retryable failure
        ExceptionHandler->>TaskService: updateStatus(FAILED)
        TaskService->>TaskRepository: save(task)

        ExceptionHandler->>NotificationService: alertSupervisor(task, reason)
        NotificationService->>NotificationService: sendAlert()

        ExceptionHandler->>Kafka: task.failed
    end

    ExceptionHandler-->>TaskService: FailureHandled
    TaskService-->>TaskController: Result
    TaskController-->>MobileApp: 200 OK
```

### Task Cancellation

```mermaid
sequenceDiagram
    autonumber
    participant Supervisor
    participant TaskController
    participant TaskService
    participant TaskRepository
    participant TaskQueueManager
    participant OperatorService
    participant EventPublisher

    Supervisor->>TaskController: DELETE /tasks/{id}
    Note over Supervisor: Cancellation reason

    TaskController->>TaskService: cancelTask(taskId, reason)

    TaskService->>TaskRepository: findById(taskId)
    TaskRepository-->>TaskService: Task

    TaskService->>TaskService: validateCanCancel()
    Note over TaskService: Cannot cancel COMPLETED

    alt Task is ASSIGNED or IN_PROGRESS
        TaskService->>OperatorService: releaseOperator(task.operatorId)
        OperatorService->>OperatorService: updateStatus(AVAILABLE)
        OperatorService-->>TaskService: Released
    else Task is PENDING
        TaskService->>TaskQueueManager: remove(task)
        TaskQueueManager-->>TaskService: Removed
    end

    TaskService->>TaskService: updateStatus(CANCELLED)
    TaskService->>TaskService: setCancellationReason(reason)

    TaskService->>TaskRepository: save(task)
    TaskRepository-->>TaskService: Task (cancelled)

    TaskService->>EventPublisher: publish(TaskCancelled)
    EventPublisher->>Kafka: task.cancelled

    TaskService-->>TaskController: TaskCancelled
    TaskController-->>Supervisor: 200 OK
```

## 6. Queue Management Operations

### Queue Status Monitoring

```mermaid
sequenceDiagram
    autonumber
    participant Dashboard
    participant TaskController
    participant TaskQueueManager
    participant Redis
    participant MetricsCalculator

    Dashboard->>TaskController: GET /queues/status
    Note over Dashboard: Warehouse and zone filters

    TaskController->>TaskQueueManager: getAllQueueStatus(warehouseId)

    TaskQueueManager->>Redis: KEYS task:queue:WH:*
    Redis-->>TaskQueueManager: Queue keys

    loop For each queue
        TaskQueueManager->>Redis: ZCARD queue
        Redis-->>TaskQueueManager: Queue depth

        TaskQueueManager->>Redis: ZRANGE queue 0 0 WITHSCORES
        Redis-->>TaskQueueManager: Oldest task and score

        TaskQueueManager->>Redis: ZRANGE queue -1 -1 WITHSCORES
        Redis-->>TaskQueueManager: Newest task and score

        TaskQueueManager->>MetricsCalculator: calculateQueueMetrics()
        MetricsCalculator-->>TaskQueueManager: QueueMetrics
    end

    TaskQueueManager-->>TaskController: List<QueueStatus>
    TaskController-->>Dashboard: 200 OK (queue statuses)
```

### Clear Stuck Queue

```mermaid
sequenceDiagram
    autonumber
    participant Admin
    participant TaskController
    participant TaskQueueManager
    participant TaskService
    participant Redis
    participant AuditService

    Admin->>TaskController: DELETE /queues/{queueKey}/clear
    Note over Admin: Admin authorization

    TaskController->>TaskQueueManager: clearQueue(warehouseId, zone, type)

    TaskQueueManager->>Redis: ZRANGE queue 0 -1
    Redis-->>TaskQueueManager: All task IDs

    TaskQueueManager->>TaskService: handleClearedTasks(taskIds)

    loop For each task
        TaskService->>TaskService: updateStatus(CANCELLED)
        TaskService->>TaskService: setReason("Queue cleared")
    end

    TaskQueueManager->>Redis: DEL queue
    Redis-->>TaskQueueManager: Deleted

    TaskQueueManager->>AuditService: logQueueClear(queue, admin, taskCount)

    TaskQueueManager->>Kafka: queue.cleared

    TaskQueueManager-->>TaskController: QueueCleared
    TaskController-->>Admin: 200 OK
```

## 7. Performance Optimization Patterns

### Batch Priority Calculation

```mermaid
sequenceDiagram
    autonumber
    participant BatchProcessor
    participant TaskService
    participant TaskPriorityCalculator
    participant CacheService
    participant TaskRepository

    BatchProcessor->>TaskService: processBatch(taskIds)
    Note over BatchProcessor: Process 100 tasks

    TaskService->>TaskRepository: findByIds(taskIds)
    TaskRepository-->>TaskService: List<Task>

    TaskService->>CacheService: getCustomerTiers(customerIds)
    Note over CacheService: Bulk fetch

    TaskService->>CacheService: getCarrierCutoffs(carriers)
    Note over CacheService: Bulk fetch

    TaskService->>CacheService: getZoneMetrics(zones)
    Note over CacheService: Bulk fetch

    CacheService-->>TaskService: CachedData

    par Parallel Priority Calculation
        loop For each task chunk (10 tasks)
            TaskService->>TaskPriorityCalculator: calculateBatch(chunk, cachedData)
            TaskPriorityCalculator-->>TaskService: Priorities
        end
    end

    TaskService->>TaskRepository: batchUpdate(tasks)
    TaskRepository-->>TaskService: Updated

    TaskService-->>BatchProcessor: BatchComplete
```

## Error Handling Patterns

```mermaid
sequenceDiagram
    autonumber
    participant Client
    participant TaskController
    participant TaskService
    participant CircuitBreaker
    participant FallbackService
    participant ErrorLogger

    Client->>TaskController: POST /tasks

    TaskController->>TaskService: createTask(request)

    TaskService->>CircuitBreaker: checkRedisHealth()

    alt Circuit Open
        CircuitBreaker-->>TaskService: CircuitOpen

        TaskService->>FallbackService: useFallbackQueue()
        FallbackService->>FallbackService: queueToDatabase()
        FallbackService-->>TaskService: Queued

        TaskService->>ErrorLogger: logCircuitOpen()
    else Circuit Closed
        CircuitBreaker-->>TaskService: Healthy

        TaskService->>Redis: Enqueue task

        alt Redis timeout
            Redis--x TaskService: Timeout

            TaskService->>CircuitBreaker: recordFailure()
            TaskService->>FallbackService: useFallbackQueue()
            TaskService->>ErrorLogger: logTimeout()
        else Success
            Redis-->>TaskService: Enqueued
        end
    end

    TaskService-->>TaskController: Result
    TaskController-->>Client: Response
```

## Key Interaction Patterns

1. **Priority Queue Management**: Redis sorted sets for O(log n) operations
2. **Event-Driven Updates**: All state changes emit Kafka events
3. **Batch Processing**: Bulk operations for efficiency
4. **Circuit Breaker**: Fallback to database when Redis unavailable
5. **Retry with Backoff**: Exponential backoff for failed tasks
6. **Audit Logging**: All manual interventions are logged
7. **Performance Metrics**: Real-time tracking of queue depths and throughput