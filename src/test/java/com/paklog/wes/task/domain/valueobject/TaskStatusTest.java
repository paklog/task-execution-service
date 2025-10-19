package com.paklog.wes.task.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskStatusTest {

    @Test
    void pendingAllowsQueueAndCancelOnly() {
        assertThat(TaskStatus.PENDING.canTransitionTo(TaskStatus.QUEUED)).isTrue();
        assertThat(TaskStatus.PENDING.canTransitionTo(TaskStatus.CANCELLED)).isTrue();
        assertThat(TaskStatus.PENDING.canTransitionTo(TaskStatus.COMPLETED)).isFalse();
    }

    @Test
    void terminalStatusesAreReportedCorrectly() {
        assertThat(TaskStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(TaskStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(TaskStatus.FAILED.isTerminal()).isTrue();
        assertThat(TaskStatus.ASSIGNED.isTerminal()).isFalse();
    }

    @Test
    void validTransitionsAreEnumerated() {
        Set<TaskStatus> transitions = TaskStatus.ASSIGNED.getValidTransitions();
        assertThat(transitions).containsExactlyInAnyOrder(TaskStatus.ACCEPTED, TaskStatus.QUEUED, TaskStatus.CANCELLED);
    }

    @Test
    void ensureCanTransitionToThrowsForInvalidTransition() {
        assertThatThrownBy(() -> TaskStatus.ACCEPTED.ensureCanTransitionTo(TaskStatus.PENDING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition from ACCEPTED to PENDING");
    }
}
