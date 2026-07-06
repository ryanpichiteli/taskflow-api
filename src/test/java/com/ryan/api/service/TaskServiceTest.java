package com.ryan.api.service;

import com.ryan.api.dto.task.TaskCreateRequest;
import com.ryan.api.dto.task.TaskResponse;
import com.ryan.api.entity.Project;
import com.ryan.api.entity.Task;
import com.ryan.api.entity.User;
import com.ryan.api.enums.Role;
import com.ryan.api.exception.ForbiddenOperationException;
import com.ryan.api.mapper.TaskMapper;
import com.ryan.api.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    private TaskService taskService;

    private User owner;
    private User member;
    private User outsider;
    private Project project;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, taskMapper, projectService, userService);

        owner = userWithId("Owner", Role.USER);
        member = userWithId("Member", Role.USER);
        outsider = userWithId("Outsider", Role.USER);

        project = new Project("TaskFlow", "Desc", owner);
        project.getMembers().add(member);
    }

    @Test
    void createShouldThrowWhenCallerIsNotProjectMember() {
        when(projectService.getEntityById(any(UUID.class))).thenReturn(project);
        org.mockito.Mockito.doThrow(new ForbiddenOperationException("You are not a member of this project"))
                .when(projectService).requireMembership(project, outsider);

        TaskCreateRequest request = new TaskCreateRequest("Title", "Description", null, null, null);

        assertThatThrownBy(() -> taskService.create(UUID.randomUUID(), request, outsider))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void createShouldThrowWhenAssigneeIsNotProjectMember() {
        when(projectService.getEntityById(any(UUID.class))).thenReturn(project);
        when(userService.getEntityById(outsider.getId())).thenReturn(outsider);

        TaskCreateRequest request = new TaskCreateRequest("Title", "Description", null, null, outsider.getId());

        assertThatThrownBy(() -> taskService.create(UUID.randomUUID(), request, member))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Assignee must be a member");
    }

    @Test
    void createShouldSucceedForProjectMember() {
        when(projectService.getEntityById(any(UUID.class))).thenReturn(project);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskMapper.toResponse(any(Task.class))).thenReturn(dummyResponse());

        TaskCreateRequest request = new TaskCreateRequest("Title", "Description", null, null, null);
        TaskResponse response = taskService.create(UUID.randomUUID(), request, member);

        assertThat(response).isNotNull();
    }

    @Test
    void deleteShouldThrowWhenCallerIsNeitherCreatorNorOwnerNorAdmin() {
        Task task = new Task("Title", "Desc", project, owner);
        when(taskRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.of(task));

        assertThatThrownBy(() -> taskService.delete(UUID.randomUUID(), member))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    private User userWithId(String name, Role role) {
        User user = new User(name, name.toLowerCase() + "@taskflow.dev", "encoded", role);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private TaskResponse dummyResponse() {
        return new TaskResponse(UUID.randomUUID(), "Title", "Desc", null, null, null, null, null, null, null, null);
    }
}
