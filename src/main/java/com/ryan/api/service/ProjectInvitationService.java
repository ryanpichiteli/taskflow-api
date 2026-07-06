package com.ryan.api.service;

import com.ryan.api.dto.invitation.InvitationResponse;
import com.ryan.api.entity.Project;
import com.ryan.api.entity.ProjectInvitation;
import com.ryan.api.entity.User;
import com.ryan.api.enums.InvitationStatus;
import com.ryan.api.exception.ForbiddenOperationException;
import com.ryan.api.exception.InvitationConflictException;
import com.ryan.api.exception.ResourceNotFoundException;
import com.ryan.api.mapper.InvitationMapper;
import com.ryan.api.repository.ProjectInvitationRepository;
import com.ryan.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProjectInvitationService {

    private final ProjectInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final InvitationMapper invitationMapper;

    public ProjectInvitationService(ProjectInvitationRepository invitationRepository, UserRepository userRepository,
                                     ProjectService projectService, InvitationMapper invitationMapper) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.projectService = projectService;
        this.invitationMapper = invitationMapper;
    }

    @Transactional
    public InvitationResponse invite(UUID projectId, String email, User currentUser) {
        Project project = projectService.getEntityById(projectId);
        projectService.requireOwnerOrAdmin(project, currentUser);

        User invitedUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No account found for email: " + email + ". Ask them to register first."));

        if (project.isMember(invitedUser.getId())) {
            throw new InvitationConflictException("This user is already a member of the project");
        }

        invitationRepository.findByProjectAndInvitedUserAndStatus(project, invitedUser, InvitationStatus.PENDING)
                .ifPresent(existing -> {
                    throw new InvitationConflictException("An invitation is already pending for this user");
                });

        ProjectInvitation invitation = new ProjectInvitation(project, invitedUser, currentUser);
        return invitationMapper.toResponse(invitationRepository.save(invitation));
    }

    public Page<InvitationResponse> listForCurrentUser(InvitationStatus status, User currentUser, Pageable pageable) {
        Page<ProjectInvitation> invitations = status != null
                ? invitationRepository.findByInvitedUserAndStatus(currentUser, status, pageable)
                : invitationRepository.findByInvitedUser(currentUser, pageable);

        return invitations.map(invitationMapper::toResponse);
    }

    public Page<InvitationResponse> listForProject(UUID projectId, User currentUser, Pageable pageable) {
        Project project = projectService.getEntityById(projectId);
        projectService.requireOwnerOrAdmin(project, currentUser);

        return invitationRepository.findByProject(project, pageable).map(invitationMapper::toResponse);
    }

    @Transactional
    public InvitationResponse accept(UUID invitationId, User currentUser) {
        ProjectInvitation invitation = getPendingInvitationForCurrentUser(invitationId, currentUser);

        invitation.accept();
        invitation.getProject().getMembers().add(invitation.getInvitedUser());

        return invitationMapper.toResponse(invitation);
    }

    @Transactional
    public InvitationResponse decline(UUID invitationId, User currentUser) {
        ProjectInvitation invitation = getPendingInvitationForCurrentUser(invitationId, currentUser);
        invitation.decline();
        return invitationMapper.toResponse(invitation);
    }

    private ProjectInvitation getPendingInvitationForCurrentUser(UUID invitationId, User currentUser) {
        ProjectInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found: " + invitationId));

        if (!invitation.getInvitedUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenOperationException("This invitation does not belong to you");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new InvitationConflictException("This invitation has already been responded to");
        }

        return invitation;
    }
}
