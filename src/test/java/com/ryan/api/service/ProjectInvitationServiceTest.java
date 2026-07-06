package com.ryan.api.service;

import com.ryan.api.dto.invitation.InvitationResponse;
import com.ryan.api.entity.Project;
import com.ryan.api.entity.ProjectInvitation;
import com.ryan.api.entity.User;
import com.ryan.api.enums.InvitationStatus;
import com.ryan.api.enums.Role;
import com.ryan.api.exception.ForbiddenOperationException;
import com.ryan.api.exception.InvitationConflictException;
import com.ryan.api.exception.ResourceNotFoundException;
import com.ryan.api.mapper.InvitationMapper;
import com.ryan.api.repository.ProjectInvitationRepository;
import com.ryan.api.repository.UserRepository;
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
class ProjectInvitationServiceTest {

    @Mock
    private ProjectInvitationRepository invitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvitationMapper invitationMapper;

    private ProjectInvitationService invitationService;

    private User owner;
    private User outsider;
    private User invitee;
    private Project project;

    @BeforeEach
    void setUp() {
        owner = userWithId("Owner", Role.USER);
        outsider = userWithId("Outsider", Role.USER);
        invitee = userWithId("Invitee", Role.USER);
        project = new Project("TaskFlow", "Desc", owner);

        invitationService = new ProjectInvitationService(invitationRepository, userRepository,
                new ProjectServiceStub(project), invitationMapper);
    }

    @Test
    void inviteShouldThrowWhenCallerIsNotOwnerOrAdmin() {
        assertThatThrownBy(() -> invitationService.invite(project.getId(), invitee.getEmail(), outsider))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void inviteShouldThrowWhenEmailHasNoAccount() {
        when(userRepository.findByEmail("ghost@taskflow.dev")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invitationService.invite(project.getId(), "ghost@taskflow.dev", owner))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void inviteShouldThrowWhenUserIsAlreadyMember() {
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> invitationService.invite(project.getId(), owner.getEmail(), owner))
                .isInstanceOf(InvitationConflictException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    void inviteShouldThrowWhenPendingInvitationAlreadyExists() {
        when(userRepository.findByEmail(invitee.getEmail())).thenReturn(Optional.of(invitee));
        when(invitationRepository.findByProjectAndInvitedUserAndStatus(project, invitee, InvitationStatus.PENDING))
                .thenReturn(Optional.of(new ProjectInvitation(project, invitee, owner)));

        assertThatThrownBy(() -> invitationService.invite(project.getId(), invitee.getEmail(), owner))
                .isInstanceOf(InvitationConflictException.class)
                .hasMessageContaining("already pending");
    }

    @Test
    void inviteShouldSucceedForOwner() {
        when(userRepository.findByEmail(invitee.getEmail())).thenReturn(Optional.of(invitee));
        when(invitationRepository.findByProjectAndInvitedUserAndStatus(project, invitee, InvitationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(invitationRepository.save(any(ProjectInvitation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invitationMapper.toResponse(any(ProjectInvitation.class))).thenReturn(dummyResponse());

        InvitationResponse response = invitationService.invite(project.getId(), invitee.getEmail(), owner);

        assertThat(response).isNotNull();
    }

    @Test
    void acceptShouldThrowWhenInvitationDoesNotBelongToCaller() {
        ProjectInvitation invitation = new ProjectInvitation(project, invitee, owner);
        when(invitationRepository.findById(any(UUID.class))).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.accept(UUID.randomUUID(), outsider))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void acceptShouldAddInviteeToProjectMembers() {
        ProjectInvitation invitation = new ProjectInvitation(project, invitee, owner);
        when(invitationRepository.findById(any(UUID.class))).thenReturn(Optional.of(invitation));
        when(invitationMapper.toResponse(invitation)).thenReturn(dummyResponse());

        invitationService.accept(UUID.randomUUID(), invitee);

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(project.isMember(invitee.getId())).isTrue();
    }

    @Test
    void acceptShouldThrowWhenInvitationAlreadyResolved() {
        ProjectInvitation invitation = new ProjectInvitation(project, invitee, owner);
        invitation.decline();
        when(invitationRepository.findById(any(UUID.class))).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.accept(UUID.randomUUID(), invitee))
                .isInstanceOf(InvitationConflictException.class);
    }

    private User userWithId(String name, Role role) {
        User user = new User(name, name.toLowerCase() + "@taskflow.dev", "encoded", role);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }

    private InvitationResponse dummyResponse() {
        return new InvitationResponse(UUID.randomUUID(), project.getId(), project.getName(),
                null, null, InvitationStatus.PENDING, null, null);
    }

    /**
     * Minimal stub avoiding a real ProjectService (which needs a repository) just to
     * expose getEntityById/requireOwnerOrAdmin for this unit test.
     */
    private class ProjectServiceStub extends ProjectService {
        private final Project stubbedProject;

        ProjectServiceStub(Project stubbedProject) {
            super(null, null);
            this.stubbedProject = stubbedProject;
        }

        @Override
        Project getEntityById(UUID id) {
            return stubbedProject;
        }
    }
}
