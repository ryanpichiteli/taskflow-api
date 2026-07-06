package com.ryan.api.dto.task;

import com.ryan.api.dto.user.UserResponse;
import com.ryan.api.enums.TaskPriority;
import com.ryan.api.enums.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        UUID projectId,
        UserResponse assignee,
        UserResponse createdBy,
        Instant createdAt,
        Instant updatedAt) {
}
