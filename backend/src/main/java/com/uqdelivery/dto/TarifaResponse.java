package com.uqdelivery.dto;

import com.uqdelivery.domain.Tarifa;

import java.math.BigDecimal;

public record TarifaResponse(Long id, String nombre, BigDecimal tarifaBase, BigDecimal costoPorKm,
                             BigDecimal costoPorKg, BigDecimal recargoFragil, BigDecimal recargoUrgente,
                             BigDecimal porcentajeSeguro, boolean activa) {
    public static TarifaResponse from(Tarifa t) {
        return new TarifaResponse(t.getId(), t.getNombre(), t.getTarifaBase(), t.getCostoPorKm(),
                t.getCostoPorKg(), t.getRecargoFragil(), t.getRecargoUrgente(), t.getPorcentajeSeguro(),
                t.isActiva());
    }
}
