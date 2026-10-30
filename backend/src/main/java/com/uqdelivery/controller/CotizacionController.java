package com.uqdelivery.controller;

import com.uqdelivery.dto.CotizacionRequest;
import com.uqdelivery.dto.CotizacionResponse;
import com.uqdelivery.service.CotizacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** HU-05 · Cotizacion automatica de envios. No guarda nada: solo calcula el precio estimado. */
@RestController
@RequestMapping("/api/cotizaciones")
@PreAuthorize("hasRole('CLIENTE_EMISOR')")
@Tag(name = "Cotizaciones", description = "Cotización automática de envíos (HU-05)")
public class CotizacionController {

    private final CotizacionService service;

    public CotizacionController(CotizacionService service) {
        this.service = service;
    }

    @Operation(summary = "Cotiza un envío",
               description = "Calcula el precio estimado con la tarifa activa: tarifa base + distancia × costo por km "
                       + "+ peso facturable × costo por kg + recargos (frágil, urgente). El peso facturable es el "
                       + "mayor entre el real y el volumétrico (largo × ancho × alto / 5000). Solo CLIENTE_EMISOR.")
    @ApiResponse(responseCode = "200", description = "Desglose del precio (valor estimado)")
    @ApiResponse(responseCode = "400", description = "Faltan datos o son inválidos: lista los campos en fieldErrors, sin precio")
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    @ApiResponse(responseCode = "403", description = "El rol no es CLIENTE_EMISOR")
    @ApiResponse(responseCode = "503", description = "No hay una tarifa activa configurada")
    @PostMapping
    public CotizacionResponse cotizar(@Valid @RequestBody CotizacionRequest req) {
        return service.cotizar(req);
    }
}
