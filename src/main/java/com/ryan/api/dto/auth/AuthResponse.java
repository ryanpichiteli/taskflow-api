package com.ryan.api.dto.auth;

import com.ryan.api.dto.user.UserResponse;

public record AuthResponse(
        String token,
        String tokenType,
        UserResponse user) {

    public static AuthResponse bearer(String token, UserResponse user) {
        return new AuthResponse(token, "Bearer", user);
    }
}
