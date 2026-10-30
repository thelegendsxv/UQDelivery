package com.uqdelivery.service;

import com.uqdelivery.domain.Tarifa;
import com.uqdelivery.dto.CotizacionRequest;
import com.uqdelivery.dto.CotizacionResponse;
import com.uqdelivery.dto.CotizacionResponse.Recargo;
import com.uqdelivery.exception.TarifaNoDisponibleException;
import com.uqdelivery.repository.TarifaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * HU-05 · Cotizacion automatica de envios.
 * precio = tarifa base + km x costo por km + peso facturable x costo por kg + recargos
 *  - peso facturable = max(peso real, peso volumetrico); volumetrico = largo x ancho x alto / 5000 (cm -> kg)
 *  - recargos fijos por fragil y por urgente
 * Todo con BigDecimal; cada valor se redondea a 2 decimales con HALF_UP.
 */
@Service
public class CotizacionService {

    static final BigDecimal FACTOR_VOLUMETRICO = new BigDecimal("5000");
    static final String MONEDA = "COP";
    private static final int ESCALA = 2;

    private final TarifaRepository tarifas;
    private final DistanciaService distancia;

    public CotizacionService(TarifaRepository tarifas, DistanciaService distancia) {
        this.tarifas = tarifas;
        this.distancia = distancia;
    }

    @Transactional(readOnly = true)
    public CotizacionResponse cotizar(CotizacionRequest req) {
        Tarifa tarifa = tarifas.findFirstByActivaTrueOrderByIdAsc().orElseThrow(TarifaNoDisponibleException::new);

        BigDecimal km = distancia.calcularKm(req.origenLat(), req.origenLng(), req.destinoLat(), req.destinoLng());
        BigDecimal pesoReal = redondear(req.pesoKg());
        BigDecimal pesoVolumetrico = pesoVolumetrico(req.largoCm(), req.anchoCm(), req.altoCm());
        BigDecimal pesoFacturable = pesoReal.max(pesoVolumetrico);

        BigDecimal costoDistancia = redondear(km.multiply(tarifa.getCostoPorKm()));
        BigDecimal costoPeso = redondear(pesoFacturable.multiply(tarifa.getCostoPorKg()));

        List<Recargo> recargos = new ArrayList<>();
        if (req.fragil()) recargos.add(new Recargo("Fragil", redondear(tarifa.getRecargoFragil())));
        if (req.urgente()) recargos.add(new Recargo("Urgente", redondear(tarifa.getRecargoUrgente())));
        BigDecimal totalRecargos = recargos.stream().map(Recargo::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(ESCALA, RoundingMode.HALF_UP);

        BigDecimal tarifaBase = redondear(tarifa.getTarifaBase());
        BigDecimal total = redondear(tarifaBase.add(costoDistancia).add(costoPeso).add(totalRecargos));

        return new CotizacionResponse(tarifa.getNombre(), tarifaBase, km, redondear(tarifa.getCostoPorKm()),
                costoDistancia, pesoReal, pesoVolumetrico, pesoFacturable, redondear(tarifa.getCostoPorKg()),
                costoPeso, List.copyOf(recargos), totalRecargos, total, MONEDA, true);
    }

    static BigDecimal pesoVolumetrico(BigDecimal largoCm, BigDecimal anchoCm, BigDecimal altoCm) {
        return largoCm.multiply(anchoCm).multiply(altoCm).divide(FACTOR_VOLUMETRICO, ESCALA, RoundingMode.HALF_UP);
    }

    private static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }
}
