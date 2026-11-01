package com.uqdelivery.controller;

import com.uqdelivery.dto.LugarResponse;
import com.uqdelivery.exception.MapasNoDisponibleException;
import com.uqdelivery.service.PuntosConocidos;
import com.uqdelivery.service.TomTomMapasClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HU-04 y HU-05 · Busqueda de lugares por nombre (barrio, direccion o sitio) para llenar origen y destino. */
@RestController
@RequestMapping("/api/lugares")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Lugares", description = "Búsqueda de barrios, direcciones y sitios con autocompletado (HU-04, HU-05)")
public class LugarController {

    static final int MIN_CARACTERES = 3;
    static final int MAX_CARACTERES = 120;

    private final TomTomMapasClient mapas;

    public LugarController(TomTomMapasClient mapas) {
        this.mapas = mapas;
    }

    @Operation(summary = "Busca lugares por texto",
               description = "Devuelve hasta 6 coincidencias (título, subtítulo con barrio y ciudad, y coordenadas), "
                       + "solo de Armenia (zona urbana). Los puntos definidos por el equipo (entradas de la Universidad del Quindío) van primero y solos. Con menos de 3 letras devuelve una lista vacía.")
    @ApiResponse(responseCode = "200", description = "Lista de lugares (puede estar vacía)")
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    @ApiResponse(responseCode = "503", description = "El buscador no está configurado o el proveedor no respondió")
    @GetMapping
    public List<LugarResponse> buscar(@RequestParam(name = "q", defaultValue = "") String q) {
        String texto = q.trim();
        if (texto.length() < MIN_CARACTERES) return List.of();
        if (texto.length() > MAX_CARACTERES) texto = texto.substring(0, MAX_CARACTERES);
        List<LugarResponse> conocidos = PuntosConocidos.buscar(texto); // p. ej. las entradas de la Universidad del Quindio
        if (!conocidos.isEmpty()) return conocidos;
        if (!mapas.disponible()) {
            throw new MapasNoDisponibleException("El buscador de direcciones no está configurado. Contacta al administrador.");
        }
        return mapas.buscar(texto);
    }
}
