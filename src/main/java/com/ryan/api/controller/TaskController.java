package com.ryan.api.controller;

import com.ryan.api.dto.common.PageResponse;
import com.ryan.api.dto.task.TaskCreateRequest;
import com.ryan.api.dto.task.TaskResponse;
import com.ryan.api.dto.task.TaskStatusUpdateRequest;
import com.ryan.api.dto.task.TaskUpdateRequest;
import com.ryan.api.enums.TaskPriority;
import com.ryan.api.enums.TaskStatus;
import com.ryan.api.security.UserPrincipal;
import com.ryan.api.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Tasks", description = "Gerenciamento de tarefas dentro de projetos")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    @Operation(summary = "Cria uma tarefa em um projeto")
    public ResponseEntity<TaskResponse> create(@PathVariable UUID projectId,
                                                @Valid @RequestBody TaskCreateRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        TaskResponse response = taskService.create(projectId, request, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    @Operation(summary = "Lista tarefas de um projeto, com filtros e paginacao")
    public PageResponse<TaskResponse> list(@PathVariable UUID projectId,
                                            @RequestParam(required = false) TaskStatus status,
                                            @RequestParam(required = false) TaskPriority priority,
                                            @RequestParam(required = false) UUID assigneeId,
                                            @RequestParam(required = false) String title,
                                            @AuthenticationPrincipal UserPrincipal principal,
                                            Pageable pageable) {
        return PageResponse.from(taskService.list(projectId, status, priority, assigneeId, title,
                principal.getUser(), pageable));
    }

    @GetMapping("/api/tasks/{id}")
    @Operation(summary = "Busca uma tarefa pelo id")
    public TaskResponse getById(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.getById(id, principal.getUser());
    }

    @PutMapping("/api/tasks/{id}")
    @Operation(summary = "Atualiza os dados de uma tarefa")
    public TaskResponse update(@PathVariable UUID id, @Valid @RequestBody TaskUpdateRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.update(id, request, principal.getUser());
    }

    @PatchMapping("/api/tasks/{id}/status")
    @Operation(summary = "Atualiza somente o status de uma tarefa")
    public TaskResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody TaskStatusUpdateRequest request,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        return taskService.updateStatus(id, request, principal.getUser());
    }

    @DeleteMapping("/api/tasks/{id}")
    @Operation(summary = "Remove uma tarefa (somente criador, owner do projeto ou ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        taskService.delete(id, principal.getUser());
        return ResponseEntity.noContent().build();
    }
}
