package com.beloveddesigns.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String phone,
        String firstName,
        String lastName
) {
}
