package com.uqdelivery.service;

import com.uqdelivery.domain.Tarifa;
import com.uqdelivery.dto.TarifaRequest;
import com.uqdelivery.dto.TarifaResponse;
import com.uqdelivery.exception.BusinessRuleException;
import com.uqdelivery.exception.ResourceNotFoundException;
import com.uqdelivery.repository.TarifaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * HU-05 · Administracion de tarifas (solo ADMINISTRADOR, se controla con @PreAuthorize).
 * Regla: siempre queda exactamente una tarifa activa. Activar una desactiva las demas
 * y no se permite desactivar la tarifa activa.
 */
@Service
public class TarifaService {

    private final TarifaRepository tarifas;

    public TarifaService(TarifaRepository tarifas) {
        this.tarifas = tarifas;
    }

    @Transactional(readOnly = true)
    public List<TarifaResponse> listar() {
        return tarifas.findAllByOrderByIdAsc().stream().map(TarifaResponse::from).toList();
    }

    @Transactional
    public TarifaResponse actualizar(Long id, TarifaRequest req) {
        Tarifa tarifa = tarifas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tarifa no encontrada"));
        if (Boolean.FALSE.equals(req.activa()) && tarifa.isActiva()) {
            throw new BusinessRuleException("Debe existir una tarifa activa: activa otra antes de desactivar esta");
        }
        tarifa.setNombre(req.nombre().trim());
        tarifa.setTarifaBase(req.tarifaBase());
        tarifa.setCostoPorKm(req.costoPorKm());
        tarifa.setCostoPorKg(req.costoPorKg());
        tarifa.setRecargoFragil(req.recargoFragil());
        tarifa.setRecargoUrgente(req.recargoUrgente());
        tarifa.setPorcentajeSeguro(req.porcentajeSeguro());
        if (Boolean.TRUE.equals(req.activa())) tarifa.setActiva(true);
        TarifaResponse respuesta = TarifaResponse.from(tarifas.save(tarifa));
        if (tarifa.isActiva()) tarifas.desactivarOtras(tarifa.getId());
        return respuesta;
    }
}
