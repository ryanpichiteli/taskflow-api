package com.ryan.api.dto.project;

import com.ryan.api.dto.user.UserResponse;
import com.ryan.api.enums.ProjectStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        ProjectStatus status,
        UserResponse owner,
        List<UserResponse> members,
        Instant createdAt,
        Instant updatedAt) {
}
