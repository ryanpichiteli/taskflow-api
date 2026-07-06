package com.ryan.api.repository;

import com.ryan.api.entity.Project;
import com.ryan.api.entity.ProjectInvitation;
import com.ryan.api.entity.User;
import com.ryan.api.enums.InvitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, java.util.UUID> {

    Optional<ProjectInvitation> findByProjectAndInvitedUserAndStatus(Project project, User invitedUser, InvitationStatus status);

    Page<ProjectInvitation> findByInvitedUser(User invitedUser, Pageable pageable);

    Page<ProjectInvitation> findByInvitedUserAndStatus(User invitedUser, InvitationStatus status, Pageable pageable);

    Page<ProjectInvitation> findByProject(Project project, Pageable pageable);
}
