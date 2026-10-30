package com.uqdelivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** HU-03 · POST /api/auth/reset-password. La contrasena usa la misma politica del registro (HU-01). */
public record ReseteoContrasenaRequest(
        @NotBlank(message = "El token es obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9_-]{43}$", message = "El token no tiene un formato valido")
        String token,

        @NotBlank(message = "La contrasena es obligatoria")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$",
                message = "La contrasena debe tener entre 8 y 72 caracteres, con mayuscula, minuscula y numero")
        String password) {
}
