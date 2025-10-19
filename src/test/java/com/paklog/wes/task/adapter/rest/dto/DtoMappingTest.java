package com.paklog.wes.task.adapter.rest.dto;

import com.paklog.domain.valueobject.Priority;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.entity.PickTaskContext;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DtoMappingTest {

    @Test
    void taskResponseAndMobileResponseExposeDomainValues() {
        Location location = new Location("A", "01", "01", "01");
        var instructions = List.of(new PickTaskContext.PickInstruction("SKU-1", 1, location, "LPN-1"));
        PickTaskContext context = new PickTaskContext("WAVE-1", "ORDER-1", PickTaskContext.PickStrategy.DISCRETE, instructions);

        WorkTask task = WorkTask.create(
                TaskType.PICK,
                "WH-1",
                "ZONE-A",
                location,
                Priority.HIGH,
                "REF-1",
                Duration.ofMinutes(12),
                LocalDateTime.now().plusHours(1),
                context
        );
        task.queue();
        task.assign("WORKER-1");
        task.accept();

        TaskResponse response = TaskResponse.fromDomain(task);
        assertThat(response.taskId()).isEqualTo(task.getTaskId());
        assertThat(response.location()).isNotNull();

        MobileTaskResponse mobileResponse = MobileTaskResponse.fromDomain(task);
        assertThat(mobileResponse.taskId()).isEqualTo(task.getTaskId());
        assertThat(mobileResponse.context()).isEqualTo(task.getContext().getMetadata());

        LocationDto locationDto = LocationDto.fromDomain(location);
        assertThat(locationDto.toDomain()).isEqualTo(location);
    }

    @Test
    void simpleDtosProvideAccessors() {
        ErrorResponse error = ErrorResponse.of(404, "Not Found", "Missing", "/path");
        assertThat(error.status()).isEqualTo(404);

        CompleteTaskRequest complete = new CompleteTaskRequest(Map.of("key", "value"));
        assertThat(complete.metadata()).containsEntry("key", "value");

        FailTaskRequest fail = new FailTaskRequest("Reason");
        assertThat(fail.reason()).isEqualTo("Reason");

        RejectTaskRequest reject = new RejectTaskRequest("Busy");
        assertThat(reject.reason()).isEqualTo("Busy");

        AssignTaskRequest assign = new AssignTaskRequest("WORKER-1", true);
        assertThat(assign.workerId()).isEqualTo("WORKER-1");

        CreateTaskRequest request = new CreateTaskRequest(TaskType.PICK, "WH-1", "ZONE-A",
                new LocationDto("A", "01", "01", "01"), Priority.HIGH, "REF-1", 900,
                LocalDateTime.now().plusHours(1), Map.of("type", "PICK"));
        assertThat(request.type()).isEqualTo(TaskType.PICK);
    }
}
