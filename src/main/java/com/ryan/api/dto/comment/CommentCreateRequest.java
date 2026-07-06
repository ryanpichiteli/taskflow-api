package com.ryan.api.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(

        @NotBlank(message = "Content is required")
        @Size(max = 2000, message = "Content must be at most 2000 characters")
        String content) {
}
