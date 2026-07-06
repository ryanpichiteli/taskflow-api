package com.ryan.api.dto.comment;

import com.ryan.api.dto.user.UserResponse;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        String content,
        UUID taskId,
        UserResponse author,
        Instant createdAt) {
}
