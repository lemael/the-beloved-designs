package com.beloveddesigns.auth.dto;

import com.beloveddesigns.except.ErrorMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = ErrorMessages.INVALID_PHONE_FORMAT)
        String phone,
        @NotBlank @Size(min = 8, message = ErrorMessages.PASSWORD_TOO_SHORT) String password,
        @NotBlank String firstName,
        @NotBlank String lastName
) {
}
