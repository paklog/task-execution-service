package com.paklog.wes.task.infrastructure.assignment;

import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskRecommendationTest {

    @Test
    void scoreHelpersClassifyThresholds() {
        TaskRecommendation high = new TaskRecommendation("T1", TaskType.PICK, Priority.HIGH, "ZONE-A", 130.0, 10);
        TaskRecommendation medium = new TaskRecommendation("T2", TaskType.PICK, Priority.NORMAL, "ZONE-A", 100.0, 20);
        TaskRecommendation low = new TaskRecommendation("T3", TaskType.PICK, Priority.LOW, "ZONE-A", 50.0, 30);

        assertThat(high.isHighScore()).isTrue();
        assertThat(high.isMediumScore()).isFalse();
        assertThat(medium.isMediumScore()).isTrue();
        assertThat(medium.isHighScore()).isFalse();
        assertThat(low.isLowScore()).isTrue();
    }
}
