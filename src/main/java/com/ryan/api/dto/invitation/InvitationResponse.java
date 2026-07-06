package com.ryan.api.dto.invitation;

import com.ryan.api.dto.user.UserResponse;
import com.ryan.api.enums.InvitationStatus;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID projectId,
        String projectName,
        UserResponse invitedUser,
        UserResponse invitedBy,
        InvitationStatus status,
        Instant createdAt,
        Instant respondedAt) {
}
