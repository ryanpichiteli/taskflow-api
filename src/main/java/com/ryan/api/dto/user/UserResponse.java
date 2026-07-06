package com.ryan.api.dto.user;

import com.ryan.api.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        Instant createdAt) {
}
