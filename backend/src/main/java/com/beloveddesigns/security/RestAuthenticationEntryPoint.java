package com.beloveddesigns.security;

import com.beloveddesigns.except.ApiError;
import com.beloveddesigns.except.ErrorMessages;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

/**
 * Route protégée appelée sans jeton : 401 UNAUTHORIZED au format {@link ApiError}
 * (par défaut Spring Security renvoyait un 403 vide, indiscernable d'un manque de droits).
 * Un client connecté sans le rôle requis reçoit toujours 403.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = new ApiError(
                Instant.now(),
                HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHORIZED",
                ErrorMessages.AUTHENTICATION_REQUIRED,
                request.getRequestURI(),
                null
        );
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
