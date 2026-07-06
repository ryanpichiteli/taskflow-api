package com.ryan.api.service;

import com.ryan.api.dto.task.TaskCreateRequest;
import com.ryan.api.dto.task.TaskResponse;
import com.ryan.api.dto.task.TaskStatusUpdateRequest;
import com.ryan.api.dto.task.TaskUpdateRequest;
import com.ryan.api.entity.Project;
import com.ryan.api.entity.Task;
import com.ryan.api.entity.User;
import com.ryan.api.enums.Role;
import com.ryan.api.enums.TaskPriority;
import com.ryan.api.enums.TaskStatus;
import com.ryan.api.exception.ForbiddenOperationException;
import com.ryan.api.exception.ResourceNotFoundException;
import com.ryan.api.mapper.TaskMapper;
import com.ryan.api.repository.TaskRepository;
import com.ryan.api.repository.spec.TaskSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final ProjectService projectService;
    private final UserService userService;

    public TaskService(TaskRepository taskRepository, TaskMapper taskMapper,
                        ProjectService projectService, UserService userService) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
        this.projectService = projectService;
        this.userService = userService;
    }

    @Transactional
    public TaskResponse create(UUID projectId, TaskCreateRequest request, User currentUser) {
        Project project = projectService.getEntityById(projectId);
        projectService.requireMembership(project, currentUser);

        Task task = new Task(request.title(), request.description(), project, currentUser);
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.MEDIUM);
        task.setDueDate(request.dueDate());

        if (request.assigneeId() != null) {
            task.setAssignee(resolveAssignee(project, request.assigneeId()));
        }

        return taskMapper.toResponse(taskRepository.save(task));
    }

    public Page<TaskResponse> list(UUID projectId, TaskStatus status, TaskPriority priority,
                                    UUID assigneeId, String title, User currentUser, Pageable pageable) {
        Project project = projectService.getEntityById(projectId);
        projectService.requireMembership(project, currentUser);

        Specification<Task> spec = com.ryan.api.repository.spec.SpecificationUtils.allOf(
                TaskSpecifications.belongsToProject(project),
                TaskSpecifications.hasStatus(status),
                TaskSpecifications.hasPriority(priority),
                TaskSpecifications.hasAssignee(assigneeId),
                TaskSpecifications.titleContains(title));

        return taskRepository.findAll(spec, pageable).map(taskMapper::toResponse);
    }

    public TaskResponse getById(UUID id, User currentUser) {
        Task task = getEntityById(id);
        projectService.requireMembership(task.getProject(), currentUser);
        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse update(UUID id, TaskUpdateRequest request, User currentUser) {
        Task task = getEntityById(id);
        projectService.requireMembership(task.getProject(), currentUser);

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority() != null ? request.priority() : task.getPriority());
        task.setDueDate(request.dueDate());

        if (request.assigneeId() != null) {
            task.setAssignee(resolveAssignee(task.getProject(), request.assigneeId()));
        } else {
            task.setAssignee(null);
        }

        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse updateStatus(UUID id, TaskStatusUpdateRequest request, User currentUser) {
        Task task = getEntityById(id);
        projectService.requireMembership(task.getProject(), currentUser);
        task.setStatus(request.status());
        return taskMapper.toResponse(task);
    }

    @Transactional
    public void delete(UUID id, User currentUser) {
        Task task = getEntityById(id);
        boolean isOwnerOrCreator = task.getProject().getOwner().getId().equals(currentUser.getId())
                || task.getCreatedBy().getId().equals(currentUser.getId());

        if (currentUser.getRole() != Role.ADMIN && !isOwnerOrCreator) {
            throw new ForbiddenOperationException("Only the task creator or project owner can delete this task");
        }

        taskRepository.delete(task);
    }

    Task getEntityById(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    private User resolveAssignee(Project project, UUID assigneeId) {
        User assignee = userService.getEntityById(assigneeId);
        if (!project.isMember(assignee.getId())) {
            throw new ForbiddenOperationException("Assignee must be a member of the project");
        }
        return assignee;
    }
}
