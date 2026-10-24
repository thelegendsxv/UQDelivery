package com.uqdelivery.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Pattern(regexp = "^[\\p{L} .'-]{2,100}$",
                 message = "El nombre solo admite letras, espacios, punto, apostrofe y guion (2 a 100 caracteres)")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,72}$",
                 message = "La contrasena debe tener entre 8 y 72 caracteres, con mayuscula, minuscula y numero")
        String password) {
}
