package com.paklog.wes.task.adapter.rest;

import com.paklog.wes.task.adapter.rest.dto.MobileTaskResponse;
import com.paklog.wes.task.adapter.rest.dto.RejectTaskRequest;
import com.paklog.wes.task.application.service.TaskManagementService;
import com.paklog.wes.task.domain.aggregate.WorkTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mobile API controller for warehouse workers
 * Simplified endpoints optimized for mobile devices
 */
@RestController
@RequestMapping("/api/v1/mobile/tasks")
@Tag(name = "Mobile API", description = "Simplified task API for mobile warehouse workers")
public class MobileTaskController {

    private final TaskManagementService taskService;

    public MobileTaskController(TaskManagementService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/my-tasks")
    @Operation(summary = "Get my tasks", description = "Get all tasks assigned to the current worker")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully")
    })
    public ResponseEntity<List<MobileTaskResponse>> getMyTasks(
            @Parameter(description = "Worker ID", required = true)
            @RequestHeader("X-Worker-Id") String workerId
    ) {
        List<WorkTask> tasks = taskService.findActiveTasksByWorker(workerId);
        List<MobileTaskResponse> response = tasks.stream()
                .map(MobileTaskResponse::fromDomain)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "Get task details", description = "Get detailed task information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task found"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<MobileTaskResponse> getTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId
    ) {
        WorkTask task = taskService.findTaskById(taskId);
        return ResponseEntity.ok(MobileTaskResponse.fromDomain(task));
    }

    @PostMapping("/{taskId}/accept")
    @Operation(summary = "Accept task", description = "Accept an assigned task")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task accepted"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "409", description = "Task cannot be accepted")
    })
    public ResponseEntity<MobileTaskResponse> acceptTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,

            @Parameter(description = "Worker ID", required = true)
            @RequestHeader("X-Worker-Id") String workerId
    ) {
        WorkTask task = taskService.acceptTask(taskId);
        return ResponseEntity.ok(MobileTaskResponse.fromDomain(task));
    }

    @PostMapping("/{taskId}/reject")
    @Operation(summary = "Reject task", description = "Reject an assigned task")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task rejected"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "409", description = "Task cannot be rejected")
    })
    public ResponseEntity<MobileTaskResponse> rejectTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,

            @Parameter(description = "Worker ID", required = true)
            @RequestHeader("X-Worker-Id") String workerId,

            @Valid @RequestBody RejectTaskRequest request
    ) {
        WorkTask task = taskService.rejectTask(taskId, request.reason());
        return ResponseEntity.ok(MobileTaskResponse.fromDomain(task));
    }

    @PostMapping("/{taskId}/start")
    @Operation(summary = "Start task", description = "Start executing an accepted task")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task started"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "409", description = "Task cannot be started")
    })
    public ResponseEntity<MobileTaskResponse> startTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,

            @Parameter(description = "Worker ID", required = true)
            @RequestHeader("X-Worker-Id") String workerId
    ) {
        WorkTask task = taskService.startTask(taskId);
        return ResponseEntity.ok(MobileTaskResponse.fromDomain(task));
    }

    @PostMapping("/{taskId}/complete")
    @Operation(summary = "Complete task", description = "Complete a task successfully")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task completed"),
            @ApiResponse(responseCode = "404", description = "Task not found"),
            @ApiResponse(responseCode = "409", description = "Task cannot be completed")
    })
    public ResponseEntity<MobileTaskResponse> completeTask(
            @Parameter(description = "Task ID", required = true)
            @PathVariable String taskId,

            @Parameter(description = "Worker ID", required = true)
            @RequestHeader("X-Worker-Id") String workerId
    ) {
        WorkTask task = taskService.completeTask(taskId);
        return ResponseEntity.ok(MobileTaskResponse.fromDomain(task));
    }
}
