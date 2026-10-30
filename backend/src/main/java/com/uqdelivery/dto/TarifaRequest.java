package com.uqdelivery.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** HU-05 · Edicion de una tarifa por el administrador. activa es opcional (null = no cambia). */
public record TarifaRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
        @Pattern(regexp = "^[^<>]*$", message = "El nombre contiene caracteres no permitidos")
        String nombre,

        @NotNull(message = "La tarifa base es obligatoria")
        @PositiveOrZero(message = "La tarifa base no puede ser negativa")
        @Digits(integer = 10, fraction = 2, message = "La tarifa base admite hasta 10 enteros y 2 decimales")
        BigDecimal tarifaBase,

        @NotNull(message = "El costo por km es obligatorio")
        @PositiveOrZero(message = "El costo por km no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El costo por km admite hasta 10 enteros y 2 decimales")
        BigDecimal costoPorKm,

        @NotNull(message = "El costo por kg es obligatorio")
        @PositiveOrZero(message = "El costo por kg no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El costo por kg admite hasta 10 enteros y 2 decimales")
        BigDecimal costoPorKg,

        @NotNull(message = "El recargo por fragil es obligatorio")
        @PositiveOrZero(message = "El recargo por fragil no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El recargo por fragil admite hasta 10 enteros y 2 decimales")
        BigDecimal recargoFragil,

        @NotNull(message = "El recargo por urgente es obligatorio")
        @PositiveOrZero(message = "El recargo por urgente no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El recargo por urgente admite hasta 10 enteros y 2 decimales")
        BigDecimal recargoUrgente,

        @NotNull(message = "El porcentaje de seguro es obligatorio")
        @PositiveOrZero(message = "El porcentaje de seguro no puede ser negativo")
        @DecimalMax(value = "100", message = "El porcentaje de seguro no puede superar 100")
        @Digits(integer = 3, fraction = 2, message = "El porcentaje de seguro admite 2 decimales")
        BigDecimal porcentajeSeguro,

        Boolean activa) {
}
