package com.paklog.wes.task.application.service;

import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.wes.task.application.command.CreateTaskCommand;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.entity.PickTaskContext;
import com.paklog.wes.task.domain.repository.WorkTaskRepository;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskStatus;
import com.paklog.wes.task.domain.valueobject.TaskType;
import com.paklog.wes.task.infrastructure.queue.TaskQueueManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class TaskManagementServiceIntegrationTest {

    @Container
    private static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0.5");

    @Container
    private static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getConnectionString);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private TaskManagementService taskService;

    @Autowired
    private WorkTaskRepository taskRepository;

    @Autowired
    private TaskQueueManager queueManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private Location location;
    private PickTaskContext context;

    @BeforeEach
    void cleanData() {
        taskRepository.deleteAll();
        Optional.ofNullable(redisTemplate.getConnectionFactory())
                .ifPresent(factory -> factory.getConnection().serverCommands().flushAll());

        location = new Location("A", "01", "01", "01");
        var instructions = List.of(new PickTaskContext.PickInstruction("SKU-1", 1, location, "LPN-1"));
        context = new PickTaskContext("WAVE-1", "ORDER-1", PickTaskContext.PickStrategy.DISCRETE, instructions);
    }

    @Test
    void createTaskPersistsAggregateAndEnqueues() {
        WorkTask created = taskService.createTask(createCommand("REF-1"));

        assertThat(created.getTaskId()).isNotBlank();
        assertThat(taskRepository.findById(created.getTaskId())).isPresent();
        assertThat(queueManager.peek("WH-1", "ZONE-A", TaskType.PICK))
                .contains(created.getTaskId());
    }

    @Test
    void taskLifecycleUpdatesPersistenceAndQueue() {
        WorkTask task = taskService.createTask(createCommand("REF-2"));

        taskService.assignTask(task.getTaskId(), "WORKER-1");
        taskService.acceptTask(task.getTaskId());
        taskService.startTask(task.getTaskId());
        WorkTask completed = taskService.completeTask(task.getTaskId());

        WorkTask reloaded = taskRepository.findById(task.getTaskId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(reloaded.getCompletedAt()).isNotNull();
        assertThat(queueManager.peek("WH-1", "ZONE-A", TaskType.PICK)).isEmpty();
        assertThat(completed.getActualDuration()).isNotNull();
    }

    @Test
    void queueManagerProvidesVisibilityAndOrdering() {
        WorkTask taskOne = taskService.createTask(createCommand("REF-3"));
        WorkTask taskTwo = taskService.createTask(createCommand("REF-4"));

        assertThat(queueManager.getQueueDepth("WH-1", "ZONE-A", TaskType.PICK)).isEqualTo(2);
        assertThat(queueManager.peek("WH-1", "ZONE-A", TaskType.PICK))
                .isPresent()
                .get()
                .isIn(taskOne.getTaskId(), taskTwo.getTaskId());

        var status = queueManager.getQueueStatus("WH-1", "ZONE-A", TaskType.PICK);
        assertThat(status.depth()).isEqualTo(2);
        assertThat(queueManager.getAllQueueStatus("WH-1")).hasSize(1);

        var dequeued = queueManager.dequeue("worker-1", "WH-1", "ZONE-A", Set.of(TaskType.PICK));
        assertThat(dequeued).isPresent();
        WorkTask remainingTask = dequeued.get().equals(taskOne.getTaskId()) ? taskTwo : taskOne;
        assertThat(queueManager.getQueueDepth("WH-1", "ZONE-A", TaskType.PICK)).isEqualTo(1);

        queueManager.remove(remainingTask);
        queueManager.clearQueue("WH-1", "ZONE-A", TaskType.PICK);
        assertThat(queueManager.getQueueDepth("WH-1", "ZONE-A", TaskType.PICK)).isZero();
    }

    private CreateTaskCommand createCommand(String referenceId) {
        return new CreateTaskCommand(
                TaskType.PICK,
                "WH-1",
                "ZONE-A",
                location,
                Priority.HIGH,
                referenceId,
                Duration.ofMinutes(15),
                LocalDateTime.now().plusHours(2),
                context
        );
    }
}
