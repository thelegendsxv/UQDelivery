package com.uqdelivery.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * HU-04 · Datos para crear una solicitud de envio. Los campos del paquete y las coordenadas
 * siguen los mismos nombres y reglas que CotizacionRequest (HU-05): el servidor los usa para
 * cotizar el envio el mismo instante en que se crea, asi el precio nunca lo decide el cliente.
 */
public record CrearEnvioRequest(
        @NotNull(message = "El peso es obligatorio")
        @Positive(message = "El peso debe ser mayor que 0")
        @DecimalMax(value = "10000", message = "El peso no puede superar 10000 kg")
        BigDecimal pesoKg,

        @NotNull(message = "El largo es obligatorio")
        @Positive(message = "El largo debe ser mayor que 0")
        @DecimalMax(value = "10000", message = "El largo no puede superar 10000 cm")
        BigDecimal largoCm,

        @NotNull(message = "El ancho es obligatorio")
        @Positive(message = "El ancho debe ser mayor que 0")
        @DecimalMax(value = "10000", message = "El ancho no puede superar 10000 cm")
        BigDecimal anchoCm,

        @NotNull(message = "El alto es obligatorio")
        @Positive(message = "El alto debe ser mayor que 0")
        @DecimalMax(value = "10000", message = "El alto no puede superar 10000 cm")
        BigDecimal altoCm,

        @NotBlank(message = "La dirección de origen es obligatoria")
        @Size(max = 255, message = "La dirección de origen no puede superar 255 caracteres")
        String origenDireccion,

        @NotNull(message = "La latitud de origen es obligatoria")
        @DecimalMin(value = "-90", message = "La latitud de origen debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "La latitud de origen debe estar entre -90 y 90")
        Double origenLat,

        @NotNull(message = "La longitud de origen es obligatoria")
        @DecimalMin(value = "-180", message = "La longitud de origen debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "La longitud de origen debe estar entre -180 y 180")
        Double origenLng,

        @NotBlank(message = "La dirección de destino es obligatoria")
        @Size(max = 255, message = "La dirección de destino no puede superar 255 caracteres")
        String destinoDireccion,

        @NotNull(message = "La latitud de destino es obligatoria")
        @DecimalMin(value = "-90", message = "La latitud de destino debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "La latitud de destino debe estar entre -90 y 90")
        Double destinoLat,

        @NotNull(message = "La longitud de destino es obligatoria")
        @DecimalMin(value = "-180", message = "La longitud de destino debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "La longitud de destino debe estar entre -180 y 180")
        Double destinoLng,

        @NotBlank(message = "El nombre del receptor es obligatorio")
        @Pattern(regexp = "^[\\p{L} .'-]{2,100}$",
                message = "El nombre del receptor solo admite letras, espacios, punto, apostrofe y guion (2 a 100 caracteres)")
        String receptorNombre,

        @NotBlank(message = "El teléfono del receptor es obligatorio")
        @Pattern(regexp = "^[0-9+() -]{7,20}$",
                message = "El teléfono del receptor debe tener de 7 a 20 caracteres: números, espacios, +, ( ) o guion")
        String receptorTelefono,

        @Email(message = "El correo del receptor no tiene un formato válido")
        @Size(max = 150, message = "El correo del receptor no puede superar 150 caracteres")
        String receptorEmail,

        @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "La descripción no puede tener los caracteres < o >")
        String descripcion,

        Boolean esFragil,

        Boolean esUrgente) {

    public boolean fragil() { return Boolean.TRUE.equals(esFragil); }

    public boolean urgente() { return Boolean.TRUE.equals(esUrgente); }
}
