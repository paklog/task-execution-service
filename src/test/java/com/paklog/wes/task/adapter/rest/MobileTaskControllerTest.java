package com.paklog.wes.task.adapter.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.wes.task.adapter.rest.dto.RejectTaskRequest;
import com.paklog.wes.task.application.service.TaskManagementService;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.entity.PickTaskContext;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MobileTaskController.class)
class MobileTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskManagementService taskManagementService;

    private WorkTask workTask;

    @BeforeEach
    void setUp() {
        Location location = new Location("A", "01", "01", "01");
        var instructions = List.of(new PickTaskContext.PickInstruction("SKU-1", 1, location, "LPN-1"));
        PickTaskContext context = new PickTaskContext("WAVE-1", "ORDER-1", PickTaskContext.PickStrategy.DISCRETE, instructions);
        workTask = WorkTask.create(
                TaskType.PICK,
                "WH-1",
                "ZONE-A",
                location,
                Priority.HIGH,
                "REF-1",
                Duration.ofMinutes(10),
                LocalDateTime.now().plusHours(1),
                context
        );
        workTask.queue();
    }

    @Test
    void getMyTasksReturnsActiveAssignments() throws Exception {
        when(taskManagementService.findActiveTasksByWorker("WORKER-1")).thenReturn(List.of(workTask));

        mockMvc.perform(get("/api/v1/mobile/tasks/my-tasks").header("X-Worker-Id", "WORKER-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(workTask.getTaskId()));
    }

    @Test
    void acceptTaskDelegatesToService() throws Exception {
        workTask.assign("WORKER-1");
        when(taskManagementService.acceptTask(workTask.getTaskId())).thenReturn(workTask);

        mockMvc.perform(post("/api/v1/mobile/tasks/{id}/accept", workTask.getTaskId())
                        .header("X-Worker-Id", "WORKER-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(workTask.getTaskId()));

        verify(taskManagementService).acceptTask(workTask.getTaskId());
    }

    @Test
    void rejectTaskReturnsUpdatedTask() throws Exception {
        workTask.assign("WORKER-1");
        when(taskManagementService.rejectTask(eq(workTask.getTaskId()), eq("Busy")))
                .thenReturn(workTask);

        mockMvc.perform(post("/api/v1/mobile/tasks/{id}/reject", workTask.getTaskId())
                        .header("X-Worker-Id", "WORKER-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new RejectTaskRequest("Busy"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(workTask.getTaskId()));
    }

    @Test
    void startTaskDelegatesToService() throws Exception {
        workTask.assign("WORKER-1");
        workTask.accept();
        when(taskManagementService.startTask(workTask.getTaskId())).thenReturn(workTask);

        mockMvc.perform(post("/api/v1/mobile/tasks/{id}/start", workTask.getTaskId())
                        .header("X-Worker-Id", "WORKER-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(workTask.getTaskId()));
    }

    @Test
    void completeTaskDelegatesToService() throws Exception {
        workTask.assign("WORKER-1");
        workTask.accept();
        workTask.start();
        when(taskManagementService.completeTask(workTask.getTaskId())).thenReturn(workTask);

        mockMvc.perform(post("/api/v1/mobile/tasks/{id}/complete", workTask.getTaskId())
                        .header("X-Worker-Id", "WORKER-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(workTask.getTaskId()));
    }
}
