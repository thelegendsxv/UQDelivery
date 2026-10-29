package com.uqdelivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarPerfilRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Pattern(regexp = "^[\\p{L} .'-]{2,100}$",
                message = "El nombre solo admite letras, espacios, punto, apostrofe y guion (2 a 100 caracteres)")
        String nombre,

        @Pattern(regexp = "^$|^[0-9+() -]{7,20}$",
                message = "El telefono solo admite numeros, espacios, +, ( ) y - (7 a 20 caracteres)")
        String telefono,

        @Size(max = 200, message = "La direccion no puede superar 200 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "La direccion contiene caracteres no permitidos")
        String direccion) {
}
