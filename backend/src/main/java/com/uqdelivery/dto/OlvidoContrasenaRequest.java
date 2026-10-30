package com.uqdelivery.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** HU-03 · POST /api/auth/forgot-password */
public record OlvidoContrasenaRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String email) {
}
