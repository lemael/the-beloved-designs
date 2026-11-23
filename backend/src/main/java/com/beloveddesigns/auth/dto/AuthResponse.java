package com.beloveddesigns.auth.dto;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String phone,
        String firstName,
        String lastName,
        List<String> roles
) {
}
