package com.ryan.api.dto.project;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddMemberRequest(

        @NotNull(message = "User id is required")
        UUID userId) {
}
