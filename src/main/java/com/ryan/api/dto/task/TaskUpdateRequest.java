package com.ryan.api.dto.task;

import com.ryan.api.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record TaskUpdateRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 3000, message = "Description must be at most 3000 characters")
        String description,

        TaskPriority priority,

        LocalDate dueDate,

        UUID assigneeId) {
}
