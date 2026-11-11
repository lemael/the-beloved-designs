package com.beloveddesigns.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Numéro WhatsApp invalide (format international requis, ex: +237612345678)")
        String phone,
        @NotBlank @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères") String password,
        @NotBlank String firstName,
        @NotBlank String lastName
) {
}
