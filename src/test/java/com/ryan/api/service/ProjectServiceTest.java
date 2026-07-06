package com.ryan.api.service;

import com.ryan.api.dto.project.ProjectResponse;
import com.ryan.api.dto.project.ProjectUpdateRequest;
import com.ryan.api.entity.Project;
import com.ryan.api.entity.User;
import com.ryan.api.enums.ProjectStatus;
import com.ryan.api.enums.Role;
import com.ryan.api.exception.ForbiddenOperationException;
import com.ryan.api.exception.ResourceNotFoundException;
import com.ryan.api.mapper.ProjectMapper;
import com.ryan.api.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private UserService userService;

    private ProjectService projectService;

    private User owner;
    private User outsider;
    private User admin;
    private Project project;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, projectMapper, userService);

        owner = userWithId("Owner", Role.USER);
        outsider = userWithId("Outsider", Role.USER);
        admin = userWithId("Admin", Role.ADMIN);
        project = new Project("TaskFlow", "A cool project", owner);
    }

    @Test
    void getByIdShouldThrowForNonMember() {
        when(projectRepository.findById(any(UUID.class))).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.getById(UUID.randomUUID(), outsider))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void getByIdShouldSucceedForAdminEvenWithoutMembership() {
        when(projectRepository.findById(any(UUID.class))).thenReturn(Optional.of(project));
        when(projectMapper.toResponse(project)).thenReturn(dummyResponse());

        assertThat(projectService.getById(UUID.randomUUID(), admin)).isNotNull();
    }

    @Test
    void updateShouldThrowWhenCallerIsNotOwnerOrAdmin() {
        when(projectRepository.findById(any(UUID.class))).thenReturn(Optional.of(project));

        ProjectUpdateRequest request = new ProjectUpdateRequest("New name", "New description", ProjectStatus.ACTIVE);

        assertThatThrownBy(() -> projectService.update(UUID.randomUUID(), request, outsider))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void updateShouldSucceedForOwner() {
        when(projectRepository.findById(any(UUID.class))).thenReturn(Optional.of(project));
        when(projectMapper.toResponse(project)).thenReturn(dummyResponse());

        ProjectUpdateRequest request = new ProjectUpdateRequest("New name", "New description", ProjectStatus.ARCHIVED);
        projectService.update(UUID.randomUUID(), request, owner);

        assertThat(project.getName()).isEqualTo("New name");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    void getEntityByIdShouldThrowWhenProjectMissing() {
        when(projectRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getById(UUID.randomUUID(), owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private User userWithId(String name, Role role) {
        User user = new User(name, name.toLowerCase() + "@taskflow.dev", "encoded", role);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private ProjectResponse dummyResponse() {
        return new ProjectResponse(UUID.randomUUID(), "TaskFlow", "A cool project",
                ProjectStatus.ACTIVE, null, java.util.List.of(), null, null);
    }
}
