package com.uqdelivery.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * HU-05 · Datos para cotizar. Los nombres siguen los de la entidad Envio (pesoKg, origenLat...).
 * esFragil y esUrgente son opcionales (null = false).
 */
public record CotizacionRequest(
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

        @NotNull(message = "La latitud de origen es obligatoria")
        @DecimalMin(value = "-90", message = "La latitud de origen debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "La latitud de origen debe estar entre -90 y 90")
        Double origenLat,

        @NotNull(message = "La longitud de origen es obligatoria")
        @DecimalMin(value = "-180", message = "La longitud de origen debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "La longitud de origen debe estar entre -180 y 180")
        Double origenLng,

        @NotNull(message = "La latitud de destino es obligatoria")
        @DecimalMin(value = "-90", message = "La latitud de destino debe estar entre -90 y 90")
        @DecimalMax(value = "90", message = "La latitud de destino debe estar entre -90 y 90")
        Double destinoLat,

        @NotNull(message = "La longitud de destino es obligatoria")
        @DecimalMin(value = "-180", message = "La longitud de destino debe estar entre -180 y 180")
        @DecimalMax(value = "180", message = "La longitud de destino debe estar entre -180 y 180")
        Double destinoLng,

        Boolean esFragil,

        Boolean esUrgente) {

    public boolean fragil() { return Boolean.TRUE.equals(esFragil); }

    public boolean urgente() { return Boolean.TRUE.equals(esUrgente); }
}
