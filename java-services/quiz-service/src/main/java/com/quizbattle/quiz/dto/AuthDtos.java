package com.quizbattle.quiz.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Request/response payloads for the authentication endpoints. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @Size(max = 80) String displayName
    ) {
    }

    public record LoginRequest(
            @NotBlank String usernameOrEmail,
            @NotBlank String password
    ) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {
    }

    /** Token pair returned on register/login/refresh. */
    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserProfile user
    ) {
    }

    public record UserProfile(
            Long id,
            String username,
            String email,
            String displayName,
            String avatarUrl,
            long xp,
            Set<String> roles
    ) {
    }
}
