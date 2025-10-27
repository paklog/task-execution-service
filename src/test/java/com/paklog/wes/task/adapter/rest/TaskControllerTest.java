package com.paklog.wes.task.adapter.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.task.execution.domain.valueobject.Priority;
import com.paklog.wes.task.adapter.rest.dto.AssignTaskRequest;
import com.paklog.wes.task.adapter.rest.mapper.TaskContextMapper;
import com.paklog.wes.task.application.command.CreateTaskCommand;
import com.paklog.wes.task.application.service.TaskManagementService;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.entity.PickTaskContext;
import com.paklog.wes.task.domain.valueobject.Location;
import com.paklog.wes.task.domain.valueobject.TaskStatus;
import com.paklog.wes.task.domain.valueobject.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskManagementService taskManagementService;

    @MockBean
    private TaskContextMapper taskContextMapper;

    private WorkTask workTask;
    private PickTaskContext context;
    private Location location;

    @BeforeEach
    void setUp() {
        location = new Location("A", "01", "01", "01");
        var instructions = List.of(new PickTaskContext.PickInstruction("SKU-1", 1, location, "LPN-1"));
        context = new PickTaskContext("WAVE-1", "ORDER-1", PickTaskContext.PickStrategy.DISCRETE, instructions);
        workTask = WorkTask.create(
                TaskType.PICK,
                "WH-1",
                "ZONE-A",
                location,
                Priority.HIGH,
                "REF-1",
                Duration.ofMinutes(15),
                LocalDateTime.now().plusHours(2),
                context
        );
        workTask.queue();
    }

    @Test
    void createTaskReturnsCreatedResponse() throws Exception {
        Map<String, Object> contextPayload = Map.of(
                "type", "PICK",
                "waveId", "WAVE-1",
                "orderId", "ORDER-1",
                "strategy", "DISCRETE",
                "instructions", List.of(Map.of("sku", "SKU-1", "quantity", 1,
                        "location", Map.of("aisle", "A", "bay", "01", "level", "01", "position", "01")))
        );

        when(taskContextMapper.mapContext(eq(TaskType.PICK), eq(contextPayload))).thenReturn(context);
        when(taskManagementService.createTask(any(CreateTaskCommand.class))).thenReturn(workTask);

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "PICK",
                                "warehouseId", "WH-1",
                                "zone", "ZONE-A",
                                "location", Map.of("aisle", "A", "bay", "01", "level", "01", "position", "01"),
                                "priority", "HIGH",
                                "referenceId", "REF-1",
                                "estimatedDurationSeconds", 900,
                                "context", contextPayload
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(workTask.getTaskId()))
                .andExpect(jsonPath("$.type").value("PICK"));

        ArgumentCaptor<CreateTaskCommand> captor = ArgumentCaptor.forClass(CreateTaskCommand.class);
        verify(taskManagementService).createTask(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(TaskType.PICK);
    }

    @Test
    void getTaskReturnsTaskDetails() throws Exception {
        workTask.assign("WORKER-1");
        when(taskManagementService.findTaskById(workTask.getTaskId())).thenReturn(workTask);

        mockMvc.perform(get("/api/v1/tasks/{id}", workTask.getTaskId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTo").value("WORKER-1"));
    }

    @Test
    void queryTasksByStatusDelegatesToService() throws Exception {
        when(taskManagementService.findTasksByStatus(TaskStatus.QUEUED)).thenReturn(List.of(workTask));

        mockMvc.perform(get("/api/v1/tasks").param("status", "QUEUED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(workTask.getTaskId()));

        verify(taskManagementService).findTasksByStatus(TaskStatus.QUEUED);
    }

    @Test
    void assignTaskReturnsUpdatedTask() throws Exception {
        workTask.assign("WORKER-1");
        when(taskManagementService.assignTask(eq(workTask.getTaskId()), eq("WORKER-1")))
                .thenReturn(workTask);

        mockMvc.perform(post("/api/v1/tasks/{id}/assign", workTask.getTaskId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignTaskRequest("WORKER-1", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTo").value("WORKER-1"));
    }

    @Test
    void getOverdueTasksReturnsCollection() throws Exception {
        when(taskManagementService.findOverdueTasks()).thenReturn(List.of(workTask));

        mockMvc.perform(get("/api/v1/tasks/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(workTask.getTaskId()));
    }

    @Test
    void getTaskReturnsNotFoundWhenMissing() throws Exception {
        when(taskManagementService.findTaskById("missing"))
                .thenThrow(new TaskManagementService.TaskNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/tasks/{id}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Task Not Found"));
    }

    @Test
    void assignTaskHandlesIllegalState() throws Exception {
        when(taskManagementService.assignTask(workTask.getTaskId(), "WORKER-1"))
                .thenThrow(new IllegalStateException("Conflict"));

        mockMvc.perform(post("/api/v1/tasks/{id}/assign", workTask.getTaskId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignTaskRequest("WORKER-1", false))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void createTaskValidationErrorReturnsBadRequest() throws Exception {
        Map<String, Object> payload = Map.of(
                "type", "PICK",
                "warehouseId", "WH-1",
                "context", Map.of("type", "PICK")
        );

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void createTaskHandlesIllegalArgument() throws Exception {
        Map<String, Object> payload = Map.of(
                "type", "PICK",
                "warehouseId", "WH-1",
                "zone", "ZONE-A",
                "referenceId", "REF-1",
                "context", Map.of("type", "PICK")
        );

        when(taskContextMapper.mapContext(eq(TaskType.PICK), any(Map.class))).thenReturn(context);
        when(taskManagementService.createTask(any(CreateTaskCommand.class)))
                .thenThrow(new IllegalArgumentException("Invalid"));

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void queryTasksHandlesUnexpectedError() throws Exception {
        when(taskManagementService.findTasksByStatus(TaskStatus.QUEUED))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/tasks").param("status", "QUEUED"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}
