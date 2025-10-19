package com.paklog.wes.task.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskTypeTest {

    @Test
    void requiresLocationForInventoryTasks() {
        assertThat(TaskType.PICK.requiresLocation()).isTrue();
        assertThat(TaskType.PUTAWAY.requiresLocation()).isTrue();
        assertThat(TaskType.PACK.requiresLocation()).isFalse();
    }

    @Test
    void identifiesOrderRelatedTasks() {
        assertThat(TaskType.PICK.isOrderRelated()).isTrue();
        assertThat(TaskType.PACK.isOrderRelated()).isTrue();
        assertThat(TaskType.REPLENISH.isOrderRelated()).isFalse();
    }

    @Test
    void providesComplexityMultiplier() {
        assertThat(TaskType.PICK.getComplexityMultiplier()).isEqualTo(1.0);
        assertThat(TaskType.REPLENISH.getComplexityMultiplier()).isGreaterThan(1.0);
        assertThat(TaskType.MOVE.getComplexityMultiplier()).isLessThan(1.0);
    }
}
