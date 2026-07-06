package com.ryan.api.service;

import com.ryan.api.dto.project.AddMemberRequest;
import com.ryan.api.dto.project.ProjectCreateRequest;
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
import com.ryan.api.repository.spec.ProjectSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final UserService userService;

    public ProjectService(ProjectRepository projectRepository, ProjectMapper projectMapper, UserService userService) {
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
        this.userService = userService;
    }

    @Transactional
    public ProjectResponse create(ProjectCreateRequest request, User owner) {
        Project project = new Project(request.name(), request.description(), owner);
        return projectMapper.toResponse(projectRepository.save(project));
    }

    public Page<ProjectResponse> list(String name, ProjectStatus status, User currentUser, Pageable pageable) {
        Specification<Project> spec = com.ryan.api.repository.spec.SpecificationUtils.allOf(
                ProjectSpecifications.nameContains(name),
                ProjectSpecifications.hasStatus(status),
                currentUser.getRole() != Role.ADMIN ? ProjectSpecifications.isMember(currentUser.getId()) : null);

        return projectRepository.findAll(spec, pageable).map(projectMapper::toResponse);
    }

    public ProjectResponse getById(UUID id, User currentUser) {
        Project project = getEntityById(id);
        requireMembership(project, currentUser);
        return projectMapper.toResponse(project);
    }

    @Transactional
    public ProjectResponse update(UUID id, ProjectUpdateRequest request, User currentUser) {
        Project project = getEntityById(id);
        requireOwnerOrAdmin(project, currentUser);

        project.setName(request.name());
        project.setDescription(request.description());
        project.setStatus(request.status());

        return projectMapper.toResponse(project);
    }

    @Transactional
    public void delete(UUID id, User currentUser) {
        Project project = getEntityById(id);
        requireOwnerOrAdmin(project, currentUser);
        projectRepository.delete(project);
    }

    @Transactional
    public ProjectResponse addMember(UUID id, AddMemberRequest request, User currentUser) {
        Project project = getEntityById(id);
        requireOwnerOrAdmin(project, currentUser);

        User newMember = userService.getEntityById(request.userId());
        project.getMembers().add(newMember);

        return projectMapper.toResponse(project);
    }

    @Transactional
    public ProjectResponse removeMember(UUID id, UUID userId, User currentUser) {
        Project project = getEntityById(id);
        requireOwnerOrAdmin(project, currentUser);

        if (project.getOwner().getId().equals(userId)) {
            throw new ForbiddenOperationException("Project owner cannot be removed from members");
        }

        project.getMembers().removeIf(member -> member.getId().equals(userId));

        return projectMapper.toResponse(project);
    }

    Project getEntityById(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
    }

    void requireMembership(Project project, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (!project.isMember(currentUser.getId())) {
            throw new ForbiddenOperationException("You are not a member of this project");
        }
    }

    private void requireOwnerOrAdmin(Project project, User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenOperationException("Only the project owner can perform this action");
        }
    }
}
