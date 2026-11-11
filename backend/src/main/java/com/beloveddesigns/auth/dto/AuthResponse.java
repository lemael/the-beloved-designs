package com.beloveddesigns.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String firstName,
        String lastName
) {
}
