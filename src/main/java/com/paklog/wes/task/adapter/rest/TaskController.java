package com.paklog.wes.task.adapter.rest;

import com.paklog.wes.task.adapter.rest.dto.*;
import com.paklog.wes.task.adapter.rest.mapper.TaskContextMapper;
import com.paklog.wes.task.application.command.CreateTaskCommand;
import com.paklog.wes.task.application.service.TaskManagementService;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import com.paklog.wes.task.domain.valueobject.TaskStatus;
import com.paklog.wes.task.domain.valueobject.TaskType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API controller for task management
 */
@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Task Management", description = "Task creation and management operations")
public class TaskController {

    private final TaskManagementService taskService;
    private final TaskContextMapper contextMapper;

    public TaskController(TaskManagementService taskService, TaskContextMapper contextMapper) {
        this.taskService = taskService;
        this.contextMapper = contextMapper;
    }

    @PostMapping
    @Operation(summary = "Create new task", description = "Create and queue a new warehouse task")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        CreateTaskCommand command = new CreateTaskCommand(
                request.type(),
                request.warehouseId(),
                request.zone(),
                request.location() != null ? request.location().toDomain() : null,
                request.priority(),
                request.referenceId(),
                request.estimatedDurationSeconds() != null
                        ? Duration.ofSeconds(request.estimatedDurationSeconds())
                        : null,
                request.deadline(),
                contextMapper.mapContext(request.type(), request.context())
        );

        WorkTask task = taskService.createTask(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TaskResponse.fromDomain(task));
    }

    @GetMapping
    @Operation(summary = "Query tasks", description = "Query tasks with optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully")
    })
    public ResponseEntity<List<TaskResponse>> queryTasks(
            @Parameter(description = "Filter by task type")
            @RequestParam(required = false) TaskType type,

            @Parameter(description = "Filter by status")
            @RequestParam(required = false) TaskStatus status,

            @Parameter(description = "Filter by assigned worker")
            @RequestParam(required = false) String assignedTo,

            @Parameter(description = "Filter by warehouse ID")
            @RequestParam(required = false) String warehouseId,

            @Parameter(description = "Filter by zone")
            @RequestParam(required = false) String zone
    ) {
        List<WorkTask> tasks;

        if (type != null && status != null) {
            tasks = taskService.findTasksByTypeAndStatus(type, status);
        } else if (warehouseId != null && status != null) {
            tasks = taskService.findTasksByWarehouseAndStatus(warehouseId, status);
        } else if (zone != null && status != null) {
            tasks = taskService.findTasksByZoneAndStatus(zone, status);
        } else if (assignedTo != null) {
            tasks = taskService.findTasksByWorker(assignedTo);
        } else if (status != null) {
            tasks = taskService.findTasksByStatus(status);
        } else {
            tasks = List.of();
        }

        List<TaskResponse> response = tasks.stream()
                .map(TaskResponse::fromDomain)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get task details", description = "Get detailed information about a task")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task found"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<TaskResponse> getTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId
    ) {
        WorkTask task = taskService.findTaskById(taskId);
        return ResponseEntity.ok(TaskResponse.fromDomain(task));
    }

    @PostMapping("/{taskId}/assign")
    @Operation(summary = "Assign task to worker", description = "Assign a task to a warehouse worker")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task assigned successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "409", description = "Task cannot be assigned")
    })
    public ResponseEntity<TaskResponse> assignTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,

            @Valid @RequestBody AssignTaskRequest request
    ) {
        WorkTask task = taskService.assignTask(taskId, request.workerId());
        return ResponseEntity.ok(TaskResponse.fromDomain(task));
    }

    @PostMapping("/{taskId}/start")
    @Operation(summary = "Start task execution", description = "Start executing an accepted task")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task started successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "409", description = "Task cannot be started")
    })
    public ResponseEntity<TaskResponse> startTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId
    ) {
        WorkTask task = taskService.startTask(taskId);
        return ResponseEntity.ok(TaskResponse.fromDomain(task));
    }

    @PostMapping("/{taskId}/complete")
    @Operation(summary = "Complete task", description = "Mark a task as successfully completed")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task completed successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "409", description = "Task cannot be completed")
    })
    public ResponseEntity<TaskResponse> completeTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,

            @RequestBody(required = false) CompleteTaskRequest request
    ) {
        WorkTask task = taskService.completeTask(taskId);
        return ResponseEntity.ok(TaskResponse.fromDomain(task));
    }

    @PostMapping("/{taskId}/fail")
    @Operation(summary = "Fail task", description = "Mark a task as failed with a reason")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task failed successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "409", description = "Task cannot be failed")
    })
    public ResponseEntity<TaskResponse> failTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,

            @Valid @RequestBody FailTaskRequest request
    ) {
        WorkTask task = taskService.failTask(taskId, request.reason());
        return ResponseEntity.ok(TaskResponse.fromDomain(task));
    }

    @PostMapping("/{taskId}/cancel")
    @Operation(summary = "Cancel task", description = "Cancel a task with a reason")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "409", description = "Task cannot be cancelled")
    })
    public ResponseEntity<TaskResponse> cancelTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,

            @Valid @RequestBody RejectTaskRequest request
    ) {
        WorkTask task = taskService.cancelTask(taskId, request.reason());
        return ResponseEntity.ok(TaskResponse.fromDomain(task));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue tasks", description = "Get all tasks that are past their deadline")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Overdue tasks retrieved")
    })
    public ResponseEntity<List<TaskResponse>> getOverdueTasks() {
        List<WorkTask> tasks = taskService.findOverdueTasks();
        List<TaskResponse> response = tasks.stream()
                .map(TaskResponse::fromDomain)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/reference/{referenceId}")
    @Operation(summary = "Get tasks by reference", description = "Get all tasks for a reference (wave, order, etc)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully")
    })
    public ResponseEntity<List<TaskResponse>> getTasksByReference(
            @Parameter(description = "Reference ID (wave, order, receipt, etc)", required = true)
            @PathVariable String referenceId
    ) {
        List<WorkTask> tasks = taskService.findTasksByReference(referenceId);
        List<TaskResponse> response = tasks.stream()
                .map(TaskResponse::fromDomain)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
