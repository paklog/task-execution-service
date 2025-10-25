package com.paklog.wes.task.domain.repository;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.entity.TaskContext;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskStatus;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataMongoTest
@Import(WorkTaskRepositoryIntegrationTest.MongoConfig.class)
class WorkTaskRepositoryIntegrationTest {

 @Container
 private static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0.5");

 @DynamicPropertySource
 static void configureProperties(DynamicPropertyRegistry registry) {
 registry.add("spring.data.mongodb.uri", mongo::getConnectionString);
 }

 @Autowired
 private WorkTaskRepository repository;

 private final Location defaultLocation = new Location("A", "01", "01", "01");

 @BeforeEach
 void clean() {
 repository.deleteAll();
 }

 @Test
 void findActiveTasksByWorkerReturnsAssignedAcceptedAndInProgress() {
 WorkTask assigned = buildTask("ASSIGNED-1", TaskType.PICK, Priority.HIGH, LocalDateTime.now().plusHours(2));
 assigned.queue();
 assigned.assign("worker-1");

 WorkTask inProgress = buildTask("INPROGRESS-1", TaskType.PACK, Priority.NORMAL, LocalDateTime.now().plusHours(1));
 inProgress.queue();
 inProgress.assign("worker-1");
 inProgress.accept();
 inProgress.start();

 WorkTask completed = buildTask("COMPLETED-1", TaskType.SHIP, Priority.NORMAL, LocalDateTime.now().minusHours(2));
 completed.queue();
 completed.assign("worker-1");
 completed.accept();
 completed.start();
 completed.complete();

 WorkTask differentWorker = buildTask("ASSIGNED-2", TaskType.PUTAWAY, Priority.LOW, LocalDateTime.now().plusHours(4));
 differentWorker.queue();
 differentWorker.assign("worker-2");

 repository.saveAll(List.of(assigned, inProgress, completed, differentWorker));

 List<WorkTask> activeForWorker = repository.findActiveTasksByWorker("worker-1");

 assertThat(activeForWorker)
 .extracting(WorkTask::getTaskId)
 .containsExactlyInAnyOrder(assigned.getTaskId(), inProgress.getTaskId());
 }

 @Test
 void findOverdueTasksReturnsOnlyOutstandingTasksPastDeadline() {
 LocalDateTime now = LocalDateTime.now();

 WorkTask overdueQueued = buildTask("OVERDUE-QUEUED", TaskType.PICK, Priority.HIGH, now.minusHours(1));
 overdueQueued.queue();

 WorkTask overdueAssigned = buildTask("OVERDUE-ASSIGNED", TaskType.PICK, Priority.HIGH, now.minusMinutes(10));
 overdueAssigned.queue();
 overdueAssigned.assign("worker-3");

 WorkTask overdueInProgress = buildTask("OVERDUE-INPROGRESS", TaskType.PICK, Priority.HIGH, now.minusMinutes(30));
 overdueInProgress.queue();
 overdueInProgress.assign("worker-3");
 overdueInProgress.accept();
 overdueInProgress.start();

 WorkTask completedOnTime = buildTask("COMPLETED-ONTIME", TaskType.PICK, Priority.NORMAL, now.minusHours(3));
 completedOnTime.queue();
 completedOnTime.assign("worker-4");
 completedOnTime.accept();
 completedOnTime.start();
 completedOnTime.complete();

 repository.saveAll(List.of(overdueQueued, overdueAssigned, overdueInProgress, completedOnTime));

 List<WorkTask> overdue = repository.findOverdueTasks(LocalDateTime.now());

 assertThat(overdue)
 .extracting(WorkTask::getTaskId)
 .containsExactlyInAnyOrder(
 overdueQueued.getTaskId(),
 overdueAssigned.getTaskId(),
 overdueInProgress.getTaskId()
 )
 .doesNotContain(completedOnTime.getTaskId());
 }

 @Test
 void countActiveTasksByWorkerCountsAllActiveStatuses() {
 WorkTask workerTask = buildTask("WORKER-ACTIVE", TaskType.REPLENISH, Priority.URGENT, LocalDateTime.now().plusHours(5));
 workerTask.queue();
 workerTask.assign("worker-5");
 workerTask.accept();

 WorkTask workerTaskTwo = buildTask("WORKER-ACTIVE-2", TaskType.REPLENISH, Priority.URGENT, LocalDateTime.now().plusHours(6));
 workerTaskTwo.queue();
 workerTaskTwo.assign("worker-5");
 workerTaskTwo.accept();
 workerTaskTwo.start();

 WorkTask cancelledTask = buildTask("CANCELLED", TaskType.REPLENISH, Priority.LOW, LocalDateTime.now().plusHours(2));
 cancelledTask.cancel("duplicate");

 repository.saveAll(List.of(workerTask, workerTaskTwo, cancelledTask));

 long activeCount = repository.countActiveTasksByWorker("worker-5");
 assertThat(activeCount).isEqualTo(2);
 }

 @Test
 void findCompletedInRangeFiltersByCompletionTime() {
 LocalDateTime start = LocalDateTime.now().minusHours(4);
 LocalDateTime end = LocalDateTime.now().plusHours(1);

 WorkTask completedInside = buildTask("COMPLETED-IN-RANGE", TaskType.PACK, Priority.NORMAL, LocalDateTime.now().minusHours(2));
 completedInside.queue();
 completedInside.assign("worker-6");
 completedInside.accept();
 completedInside.start();
 completedInside.complete();

 WorkTask completedOutside = buildTask("COMPLETED-OUT-OF-RANGE", TaskType.PACK, Priority.NORMAL, LocalDateTime.now().minusHours(5));
 completedOutside.queue();
 completedOutside.assign("worker-6");
 completedOutside.accept();
 completedOutside.start();
 completedOutside.complete();
 ReflectionTestUtils.setField(
 completedOutside,
 "completedAt",
 LocalDateTime.now().minusHours(5)
 );

 repository.saveAll(List.of(completedInside, completedOutside));

 List<WorkTask> completed = repository.findCompletedInRange(start, end);

 assertThat(completed)
 .extracting(WorkTask::getTaskId)
 .containsExactly(completedInside.getTaskId());
 }

 private WorkTask buildTask(String referenceId,
 TaskType type,
 Priority priority,
 LocalDateTime deadline) {
 WorkTask task = WorkTask.create(
 type,
 "WH-INT",
 "ZONE-INT",
 defaultLocation,
 priority,
 referenceId,
 Duration.ofMinutes(20),
 deadline,
 new SimpleContext()
 );
 task.setPriority(priority);
 task.setReferenceId(referenceId);
 return task;
 }

 private static class SimpleContext implements TaskContext {
 @Override
 public void validate() {
 // No-op
 }

 @Override
 public Map<String, Object> getMetadata() {
 return Map.of();
 }
 }

 /**
 * Empty configuration class to keep Spring context happy for @DataMongoTest with Testcontainers.
 * The annotation processor requires at least one configuration class when using @Import.
 */
 static class MongoConfig {
 }
}
