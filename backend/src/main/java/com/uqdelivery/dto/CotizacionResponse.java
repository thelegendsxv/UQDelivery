package com.uqdelivery.dto;

import java.math.BigDecimal;
import java.util.List;

/** HU-05 · Desglose del precio. Valores en COP con 2 decimales; siempre es un valor estimado. */
public record CotizacionResponse(
        String tarifa,
        BigDecimal tarifaBase,
        BigDecimal distanciaKm,
        BigDecimal costoPorKm,
        BigDecimal costoDistancia,
        BigDecimal pesoRealKg,
        BigDecimal pesoVolumetricoKg,
        BigDecimal pesoFacturableKg,
        BigDecimal costoPorKg,
        BigDecimal costoPeso,
        List<Recargo> recargos,
        BigDecimal totalRecargos,
        BigDecimal total,
        String moneda,
        boolean estimado) {

    public record Recargo(String concepto, BigDecimal valor) { }
}
