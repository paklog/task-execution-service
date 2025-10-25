package com.paklog.wes.task.domain.service;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.entity.TaskContext;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TaskPriorityCalculatorTest {

    private TaskPriorityCalculator calculator;
    private Location defaultLocation;

    @BeforeEach
    void setUp() {
        calculator = new TaskPriorityCalculator();
        defaultLocation = new Location("A", "01", "01", "01");
    }

    @Test
    void calculatePriorityRewardsSlaCutoffAndExpressSignals() {
        LocalDateTime now = LocalDateTime.now();
        WorkTask rushTask = newTask(
                Priority.URGENT,
                TaskType.SHIP,
                now.plusMinutes(45),
                Map.of(
                        "express", true,
                        "customerTier", "PLATINUM",
                        "carrierCutoffTime", now.plusMinutes(20)
                )
        );
        rushTask.setZone("ZONE-A1");
        rushTask.setCreatedAt(now.minusHours(6));

        WorkTask relaxedTask = newTask(
                Priority.NORMAL,
                TaskType.COUNT,
                now.plusHours(12),
                Map.of("customerTier", "STANDARD")
        );
        relaxedTask.setZone("ZONE-C5");
        relaxedTask.setCreatedAt(now.minusHours(1));

        int rushScore = calculator.calculatePriority(rushTask);
        int relaxedScore = calculator.calculatePriority(relaxedTask);

        assertThat(rushScore).isGreaterThan(relaxedScore);
        assertThat(rushScore).isGreaterThan(400);
    }

    @Test
    void calculatePriorityRespectsMinimumBaselinePriority() {
        LocalDateTime now = LocalDateTime.now();
        WorkTask lowContextTask = newTask(
                Priority.LOW,
                TaskType.MOVE,
                now.plusDays(2),
                Map.of()
        );
        lowContextTask.setZone("ZONE-D1");
        lowContextTask.setCreatedAt(now.minusMinutes(30));

        int computed = calculator.calculatePriority(lowContextTask);
        int elevated = calculator.calculatePriority(lowContextTask, Priority.HIGH);

        assertThat(computed).isLessThan(elevated);
        assertThat(elevated).isGreaterThanOrEqualTo(600);
    }

    @Test
    void dynamicPriorityAppliesContextualBoosts() {
        LocalDateTime now = LocalDateTime.now();
        WorkTask task = newTask(
                Priority.HIGH,
                TaskType.PICK,
                now.plusHours(2),
                Map.of(
                        "carrierCutoffTime", now.plusHours(1),
                        "customerTier", "GOLD"
                )
        );
        task.setZone("PICK-A1");
        task.setCreatedAt(now.minusHours(4));

        int base = calculator.calculatePriority(task);
        Map<String, Object> context = new HashMap<>();
        context.put("operatorInSameZone", true);
        context.put("partOfBatch", true);
        context.put("waveReleased", true);
        context.put("resolvingException", true);
        context.put("systemSurgeLevel", 2);

        int dynamic = calculator.calculateDynamicPriority(task, context);

        assertThat(dynamic)
                .isGreaterThan(base)
                .isLessThanOrEqualTo(1000);
    }

    @Test
    void recommendAdjustmentProvidesActionableReason() {
        LocalDateTime now = LocalDateTime.now();
        WorkTask task = newTask(
                Priority.LOW,
                TaskType.SHIP,
                now.plusMinutes(30),
                Map.of(
                        "express", true,
                        "customerTier", "Gold",
                        "carrierCutoffTime", now.plusMinutes(25)
                )
        );
        task.setZone("ZONE-A2");
        task.setCreatedAt(now.minusHours(10));

        TaskPriorityCalculator.SystemLoadMetrics loadMetrics =
                new TaskPriorityCalculator.SystemLoadMetrics(
                        15,
                        1,
                        3.5,
                        Map.of("ZONE-A2", 7)
                );

        TaskPriorityCalculator.PriorityAdjustment adjustment =
                calculator.recommendAdjustment(task, loadMetrics);

        assertThat(adjustment.shouldAdjust()).isTrue();
        assertThat(adjustment.getRecommendedPriority())
                .isGreaterThan(adjustment.getCurrentPriority());
        assertThat(adjustment.getReason())
                .contains("SLA")
                .contains("Carrier cutoff")
                .contains("High queue depth");
    }

    private WorkTask newTask(
            Priority priority,
            TaskType type,
            LocalDateTime deadline,
            Map<String, Object> metadata
    ) {
        WorkTask task = WorkTask.create(
                type,
                "WH-1",
                "ZONE-A",
                defaultLocation,
                priority,
                "REF-123",
                Duration.ofMinutes(30),
                deadline,
                new StubTaskContext(metadata)
        );
        task.setPriority(priority);
        return task;
    }

    private static class StubTaskContext implements TaskContext {
        private final Map<String, Object> metadata;

        private StubTaskContext(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
        }

        @Override
        public void validate() {
            // No-op for tests
        }

        @Override
        public Map<String, Object> getMetadata() {
            return metadata;
        

}
}
}
