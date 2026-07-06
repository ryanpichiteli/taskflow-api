package com.ryan.api.dto.task;

import com.ryan.api.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateRequest(

        @NotNull(message = "Status is required")
        TaskStatus status) {
}
