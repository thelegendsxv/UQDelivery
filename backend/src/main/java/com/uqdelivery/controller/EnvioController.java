package com.uqdelivery.controller;

import com.uqdelivery.dto.CrearEnvioRequest;
import com.uqdelivery.dto.EnvioResponse;
import com.uqdelivery.service.EnvioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** HU-04 · Creacion y consulta de solicitudes de envio. */
@RestController
@RequestMapping("/api/envios")
@PreAuthorize("hasRole('CLIENTE_EMISOR')")
@Tag(name = "Envíos", description = "Solicitud y consulta de envíos (HU-04)")
public class EnvioController {

    private final EnvioService service;

    public EnvioController(EnvioService service) {
        this.service = service;
    }

    @Operation(summary = "Crea una solicitud de envío",
               description = "El precio y la distancia se calculan en el servidor con la misma fórmula de HU-05 "
                       + "(no se reciben del cliente). Estado inicial PENDIENTE_ASIGNACION, con el primer registro "
                       + "en el historial de estados. Solo CLIENTE_EMISOR.")
    @ApiResponse(responseCode = "201", description = "Envío creado, con su código de seguimiento y el precio estimado")
    @ApiResponse(responseCode = "400", description = "Faltan datos o son inválidos: no se crea ningún registro")
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    @ApiResponse(responseCode = "403", description = "El rol no es CLIENTE_EMISOR")
    @ApiResponse(responseCode = "503", description = "No hay una tarifa activa configurada para cotizar")
    @PostMapping
    public ResponseEntity<EnvioResponse> crear(@Valid @RequestBody CrearEnvioRequest req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req, auth.getName()));
    }

    @Operation(summary = "Lista los envíos del cliente autenticado",
               description = "Solo los envíos que el cliente creó, del más reciente al más antiguo.")
    @ApiResponse(responseCode = "200", description = "Lista de envíos del cliente")
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    @ApiResponse(responseCode = "403", description = "El rol no es CLIENTE_EMISOR")
    @GetMapping("/mis-envios")
    public List<EnvioResponse> misEnvios(Authentication auth) {
        return service.misEnvios(auth.getName());
    }

    @Operation(summary = "Consulta un envío por su código de seguimiento",
               description = "Solo si el envío pertenece al cliente autenticado; si no, 404 (no revela si el código existe).")
    @ApiResponse(responseCode = "200", description = "Datos del envío")
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    @ApiResponse(responseCode = "403", description = "El rol no es CLIENTE_EMISOR")
    @ApiResponse(responseCode = "404", description = "No existe un envío con ese código para este cliente")
    @GetMapping("/{codigo}")
    public EnvioResponse obtener(@PathVariable String codigo, Authentication auth) {
        return service.obtenerPorCodigo(codigo, auth.getName());
    }
}
