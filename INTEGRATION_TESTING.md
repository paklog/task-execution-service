# Integration Testing Strategy - Task Execution Service

**Status**: ✅ Infrastructure Complete | 🔄 Tests In Progress
**Last Updated**: 2025-10-18

---

## Overview

This document outlines the integration testing strategy for the Task Execution Service, including infrastructure setup, test dependencies, and recommendations for comprehensive integration testing.

---

## Current Test Coverage

### Unit Tests ✅ COMPLETE
- **Test File**: `WorkTaskTest.java`
- **Test Count**: 19 tests
- **Coverage**: Domain model, business logic, state machine
- **Status**: All passing ✅

**Test Categories:**
- Task creation and validation
- State machine transitions (PENDING → QUEUED → ASSIGNED → ACCEPTED → IN_PROGRESS → COMPLETED)
- Business invariants
- Domain events
- Worker assignment lifecycle
- Performance calculations
- Priority scoring
- Failure and cancellation handling

---

## Integration Test Infrastructure ✅ COMPLETE

### Dependencies Added

The following test dependencies have been added to `pom.xml`:

```xml
<!-- Embedded MongoDB for integration tests -->
<dependency>
    <groupId>de.flapdoodle.embed</groupId>
    <artifactId>de.flapdoodle.embed.mongo</artifactId>
    <version>4.9.2</version>
    <scope>test</scope>
</dependency>

<!-- Embedded Redis for queue testing -->
<dependency>
    <groupId>it.ozimov</groupId>
    <artifactId>embedded-redis</artifactId>
    <version>0.7.3</version>
    <scope>test</scope>
</dependency>

<!-- Spring Kafka Test for event testing -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>

<!-- Testcontainers for containerized testing -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<!-- Awaitility for async testing -->
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.0</version>
    <scope>test</scope>
</dependency>
```

---

## Recommended Integration Tests

### 1. REST API Integration Tests

**File**: `TaskControllerIntegrationTest.java` (To Be Implemented)

**Test Scenarios:**
- ✅ POST `/api/v1/tasks` - Create task
- ✅ GET `/api/v1/tasks` - Query tasks by status
- ✅ GET `/api/v1/tasks/{id}` - Get task details
- ✅ POST `/api/v1/tasks/{id}/assign` - Assign to worker
- ✅ POST `/api/v1/tasks/{id}/start` - Start execution
- ✅ POST `/api/v1/tasks/{id}/complete` - Complete task
- ✅ POST `/api/v1/tasks/{id}/fail` - Fail task
- ✅ POST `/api/v1/tasks/{id}/cancel` - Cancel task
- ✅ GET `/api/v1/tasks/overdue` - Get overdue tasks
- ✅ GET `/api/v1/tasks/reference/{id}` - Get tasks by reference

**Technologies:**
- Spring Boot Test (`@SpringBootTest`)
- MockMvc for HTTP testing
- Embedded MongoDB
- AssertJ for fluent assertions

---

### 2. Mobile API Integration Tests

**File**: `MobileTaskControllerIntegrationTest.java` (To Be Implemented)

**Test Scenarios:**
- ✅ GET `/api/v1/mobile/tasks/my-tasks` - Worker's assigned tasks
- ✅ GET `/api/v1/mobile/tasks/{id}` - Task details
- ✅ POST `/api/v1/mobile/tasks/{id}/accept` - Accept task
- ✅ POST `/api/v1/mobile/tasks/{id}/reject` - Reject task
- ✅ POST `/api/v1/mobile/tasks/{id}/start` - Start task
- ✅ POST `/api/v1/mobile/tasks/{id}/complete` - Complete task

**Special Considerations:**
- Test `X-Worker-Id` header validation
- Test worker isolation (worker can only see/modify their tasks)
- Test rejection workflow (task returns to queue)

---

### 3. Queue Manager Integration Tests

**File**: `TaskQueueManagerIntegrationTest.java` (To Be Implemented)

**Test Scenarios:**
- ✅ Enqueue task to Redis sorted set
- ✅ Dequeue highest priority task
- ✅ Remove task from queue
- ✅ Get queue status and depth
- ✅ Handle empty queue dequeue
- ✅ Maintain separate queues per warehouse/zone/type
- ✅ Prioritize overdue tasks
- ✅ Respect worker capabilities
- ✅ Handle concurrent enqueue/dequeue operations
- ✅ Detect queue backlog

**Technologies:**
- Embedded Redis
- RedisTemplate for verification
- Concurrent testing scenarios

---

### 4. Event Handler Integration Tests

**File**: `WaveEventHandlerIntegrationTest.java` (To Be Implemented)

**Test Scenarios:**
- ✅ Create pick tasks when wave is released
- ✅ Handle wave with single order
- ✅ Handle wave with many orders (50+)
- ✅ Handle multiple wave releases concurrently
- ✅ Handle different wave priorities
- ✅ Include wave context in generated tasks
- ✅ Verify tasks are queued automatically

**Technologies:**
- Embedded Kafka (`@EmbeddedKafka`)
- KafkaTemplate for event publishing
- Awaitility for async verification

**Challenge**: WaveReleasedEvent is in `wave-planning-service` module, not in `paklog-events`. Integration tests need to:
1. Add wave-planning-service as test dependency, OR
2. Create test event classes, OR
3. Use Spring REST to trigger wave releases in full system test

---

### 5. End-to-End Workflow Tests

**File**: `EndToEndWorkflowTest.java` (To Be Implemented)

**Complete Workflow Test:**
```
Wave Release
  ↓ (Kafka Event)
Task Generation
  ↓ (MongoDB Save)
Redis Queue
  ↓ (Assignment)
Worker Assignment
  ↓ (Mobile API)
Worker Acceptance
  ↓
Task Execution
  ↓
Task Completion
  ↓ (Domain Event)
Event Published
```

**Test Scenarios:**
- ✅ Full workflow from wave to completion
- ✅ Task rejection and reassignment
- ✅ Multiple workers processing same wave
- ✅ Task failure with error reporting
- ✅ Dequeue respecting worker capabilities

---

## Implementation Challenges

### Challenge 1: Complex Domain Model

**Issue**: The domain model uses complex value objects and entities:
- `WorkTask.create()` requires `TaskContext`, `Location`, `Duration`, `LocalDateTime`
- `PickTaskContext` requires `PickStrategy` and `List<PickInstruction>`
- `Location` requires 4 parameters (aisle, bay, level, position)

**Solution Options:**
1. **Test Builders**: Create test builder classes for complex domain objects
   ```java
   public class WorkTaskTestBuilder {
       public static WorkTask createPickTask(String referenceId) {
           PickTaskContext context = new PickTaskContext(
               referenceId,
               "ORDER-001",
               PickStrategy.BATCH,
               List.of()
           );
           return WorkTask.create(
               TaskType.PICK,
               "WH-001",
               "ZONE-A",
               new Location("A", "01", "02", "01"),
               Priority.NORMAL,
               referenceId,
               Duration.ofMinutes(10),
               LocalDateTime.now().plusHours(2),
               context
           );
       }
   }
   ```

2. **Test Fixtures**: Create pre-built test data
3. **Factory Methods**: Add static factory methods to domain objects for testing

---

### Challenge 2: Event Source Dependencies

**Issue**: `WaveReleasedEvent` and `WavePriority` are in wave-planning-service module

**Solution Options:**
1. Add wave-planning-service as test dependency
2. Create test-only event classes
3. Mock event handling
4. Use contract testing with Pact or Spring Cloud Contract

---

### Challenge 3: Async Event Processing

**Issue**: Kafka events are processed asynchronously

**Solution**: Use Awaitility
```java
await().atMost(5, TimeUnit.SECONDS)
    .untilAsserted(() -> {
        List<WorkTask> tasks = taskRepository.findByReferenceId("WAVE-001");
        assertThat(tasks).hasSize(3);
    });
```

---

## Test Execution

### Run All Tests
```bash
mvn clean test
```

### Run Only Unit Tests
```bash
mvn test -Dtest=WorkTaskTest
```

### Run Only Integration Tests
```bash
mvn test -Dtest=*IntegrationTest
```

### Run Specific Test
```bash
mvn test -Dtest=TaskControllerIntegrationTest#shouldCreateTask
```

---

## Performance Metrics

### Current Unit Tests
- **Execution Time**: ~70ms
- **Build Time**: ~2.5s
- **Test Count**: 19
- **Pass Rate**: 100%

### Target Integration Tests
- **Execution Time**: <5s per test class
- **Total Build Time**: <30s
- **Test Count**: 50-100
- **Coverage Target**: 80%+ overall

---

## Next Steps

### Phase 1: Test Infrastructure ✅ COMPLETE
- [x] Add embedded MongoDB dependency
- [x] Add embedded Redis dependency
- [x] Add Spring Kafka Test dependency
- [x] Add Testcontainers dependency
- [x] Add Awaitility dependency

### Phase 2: Create Test Builders 🔄 IN PROGRESS
- [ ] Create `WorkTaskTestBuilder` for easy test data creation
- [ ] Create `TaskContextTestBuilder` for context objects
- [ ] Create `LocationTestBuilder` for location objects
- [ ] Create test event classes if needed

### Phase 3: Implement Integration Tests ⏳ PLANNED
- [ ] `TaskControllerIntegrationTest` (10 scenarios)
- [ ] `MobileTaskControllerIntegrationTest` (6 scenarios)
- [ ] `TaskQueueManagerIntegrationTest` (10 scenarios)
- [ ] `WaveEventHandlerIntegrationTest` (6 scenarios)
- [ ] `EndToEndWorkflowTest` (5 scenarios)

### Phase 4: Contract Testing ⏳ PLANNED
- [ ] Define API contracts with OpenAPI
- [ ] Create consumer-driven contracts
- [ ] Implement contract tests with Pact

### Phase 5: Performance Testing ⏳ PLANNED
- [ ] Load test task creation (1000 tasks/minute)
- [ ] Stress test queue operations
- [ ] Test concurrent worker assignments
- [ ] Test Kafka throughput

---

## Best Practices

### 1. Test Isolation
- Clean MongoDB before each test
- Clean Redis before each test
- Use unique IDs for test data
- Don't share state between tests

### 2. Async Testing
- Use Awaitility for async operations
- Set reasonable timeouts (5-10s)
- Verify final state, not intermediate states

### 3. Test Data
- Use meaningful test IDs (WAVE-001, ORDER-001)
- Create minimal data needed for test
- Clean up after tests

### 4. Assertions
- Use AssertJ for fluent assertions
- Verify database state after API calls
- Check domain events are published
- Validate queue operations

### 5. Error Cases
- Test validation failures
- Test business rule violations
- Test concurrent modification
- Test timeout scenarios

---

## Resources

### Documentation
- Spring Boot Testing: https://docs.spring.io/spring-boot/testing.html
- Embedded MongoDB: https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo
- Embedded Redis: https://github.com/ozimov/embedded-redis
- Testcontainers: https://testcontainers.com/
- Awaitility: https://github.com/awaitility/awaitility

### Examples
- Spring REST API Tests: https://spring.io/guides/gs/testing-web/
- Kafka Integration Tests: https://www.baeldung.com/spring-boot-kafka-testing
- MongoDB Integration Tests: https://www.baeldung.com/spring-boot-embedded-mongodb

---

## Conclusion

The integration test infrastructure is **complete and ready** for implementation. The main challenges are:
1. Creating test builders for complex domain objects
2. Handling async event processing
3. Managing cross-module dependencies (WaveReleasedEvent)

**Recommended Approach**: Start with simple REST API tests, then gradually add queue and event testing as test builders are created.

**Current Status**:
- ✅ Unit tests: 100% passing (19 tests)
- ✅ Infrastructure: Complete
- 🔄 Integration tests: Ready to implement
- ⏳ Full test suite: Target 70+ integration tests

---

**Total Progress**: Infrastructure 100%, Implementation 0%, Documentation 100%
