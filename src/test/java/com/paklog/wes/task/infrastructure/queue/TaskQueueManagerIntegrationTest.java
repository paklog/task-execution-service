package com.paklog.wes.task.infrastructure.queue;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.entity.PickTaskContext;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
@SpringBootTest(classes = TaskQueueManagerIntegrationTest.RedisTestConfig.class)
class TaskQueueManagerIntegrationTest {

    @Container
    private static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private TaskQueueManager queueManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private Location defaultLocation;

    @BeforeEach
    void cleanRedis() {
        Optional.ofNullable(redisTemplate.getConnectionFactory())
                .ifPresent(factory -> factory.getConnection().serverCommands().flushAll());
        defaultLocation = new Location("A", "01", "01", "01");
    }

    @Test
    void enqueueAndDequeuePrioritizesLowestScore() {
        WorkTask critical = newPickTask("REF-CRIT", Priority.CRITICAL, LocalDateTime.now().plusMinutes(30));
        critical.setCreatedAt(LocalDateTime.now().minusHours(4));
        critical.queue();

        WorkTask normal = newPickTask("REF-NORM", Priority.NORMAL, LocalDateTime.now().plusHours(4));
        normal.setCreatedAt(LocalDateTime.now());
        normal.queue();

        queueManager.enqueue(normal);
        queueManager.enqueue(critical);

        String firstTask = queueManager.dequeue("worker-1", "WH-Q", "ZONE-A", Set.of(TaskType.PICK))
                .orElseThrow();
        String secondTask = queueManager.dequeue("worker-1", "WH-Q", "ZONE-A", Set.of(TaskType.PICK))
                .orElseThrow();
        Optional<String> empty = queueManager.dequeue("worker-1", "WH-Q", "ZONE-A", Set.of(TaskType.PICK));

        assertThat(firstTask).isEqualTo(critical.getTaskId());
        assertThat(secondTask).isEqualTo(normal.getTaskId());
        assertThat(empty).isEmpty();
        assertThat(queueManager.getQueueDepth("WH-Q", "ZONE-A", TaskType.PICK)).isZero();
    }

    @Test
    void getAllQueueStatusIncludesOldestTaskAndFiltersInvalidKeys() {
        WorkTask older = newPickTask("REF-OLD", Priority.HIGH, LocalDateTime.now().plusMinutes(45));
        older.setCreatedAt(LocalDateTime.now().minusHours(6));
        older.queue();

        WorkTask newer = newPickTask("REF-NEW", Priority.LOW, LocalDateTime.now().plusHours(6));
        newer.setCreatedAt(LocalDateTime.now());
        newer.queue();

        queueManager.enqueue(older);
        queueManager.enqueue(newer);

        redisTemplate.opsForValue().set("task:queue:WH-Q:INVALID", "junk");

        List<QueueStatus> statuses = queueManager.getAllQueueStatus("WH-Q");

        assertThat(statuses).hasSize(1);
        QueueStatus status = statuses.getFirst();
        assertThat(status.queueKey()).isEqualTo("task:queue:WH-Q:ZONE-A:PICK");
        assertThat(status.depth()).isEqualTo(2);
        assertThat(status.oldestTaskId()).isEqualTo(older.getTaskId());
    }

    @Test
    void removeAndClearQueueUpdatesRedisState() {
        WorkTask keep = newPickTask("REF-KEEP", Priority.NORMAL, LocalDateTime.now().plusHours(2));
        keep.queue();
        WorkTask remove = newPickTask("REF-REMOVE", Priority.URGENT, LocalDateTime.now().plusMinutes(50));
        remove.queue();

        queueManager.enqueue(keep);
        queueManager.enqueue(remove);

        queueManager.remove(remove);
        assertThat(queueManager.peek("WH-Q", "ZONE-A", TaskType.PICK))
                .contains(keep.getTaskId());

        queueManager.clearQueue("WH-Q", "ZONE-A", TaskType.PICK);
        assertThat(queueManager.getQueueDepth("WH-Q", "ZONE-A", TaskType.PICK)).isZero();
        assertThat(queueManager.peek("WH-Q", "ZONE-A", TaskType.PICK)).isEmpty();
    }

    private WorkTask newPickTask(String referenceId, Priority priority, LocalDateTime deadline) {
        PickTaskContext context = new PickTaskContext(
                "WAVE-" + referenceId,
                "ORDER-" + referenceId,
                PickTaskContext.PickStrategy.DISCRETE,
                List.of(new PickTaskContext.PickInstruction(
                        "SKU-" + referenceId,
                        1,
                        defaultLocation,
                        "LPN-" + referenceId
                ))
        );

        WorkTask task = WorkTask.create(
                TaskType.PICK,
                "WH-Q",
                "ZONE-A",
                defaultLocation,
                priority,
                referenceId,
                Duration.ofMinutes(10),
                deadline,
                context
        );
        task.setPriority(priority);
        return task;
    }

    @Configuration
    @EnableAutoConfiguration(exclude = {
            KafkaAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class,
            MongoRepositoriesAutoConfiguration.class
    })
    @Import(TaskQueueManager.class)
    static class RedisTestConfig {
    }
}
