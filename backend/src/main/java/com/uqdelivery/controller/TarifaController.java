package com.uqdelivery.controller;

import com.uqdelivery.dto.TarifaRequest;
import com.uqdelivery.dto.TarifaResponse;
import com.uqdelivery.service.TarifaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** HU-05 · Administracion de las tarifas con las que se cotiza. */
@RestController
@RequestMapping("/api/tarifas")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@Tag(name = "Tarifas", description = "Configuración de tarifas de cotización (HU-05, solo ADMINISTRADOR)")
public class TarifaController {

    private final TarifaService service;

    public TarifaController(TarifaService service) {
        this.service = service;
    }

    @Operation(summary = "Lista las tarifas", description = "Incluye cuál está activa. Solo ADMINISTRADOR.")
    @ApiResponse(responseCode = "200", description = "Lista de tarifas")
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    @ApiResponse(responseCode = "403", description = "El rol no es ADMINISTRADOR")
    @GetMapping
    public List<TarifaResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Actualiza una tarifa",
               description = "Valores en COP, no negativos. Si activa = true, las demás quedan inactivas; "
                       + "no se puede desactivar la tarifa activa (siempre debe haber una).")
    @ApiResponse(responseCode = "200", description = "Tarifa actualizada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos (devuelve los campos con error)")
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    @ApiResponse(responseCode = "403", description = "El rol no es ADMINISTRADOR")
    @ApiResponse(responseCode = "404", description = "La tarifa no existe")
    @ApiResponse(responseCode = "409", description = "Se intentó dejar el sistema sin tarifa activa")
    @PutMapping("/{id}")
    public TarifaResponse actualizar(@PathVariable Long id, @Valid @RequestBody TarifaRequest req) {
        return service.actualizar(id, req);
    }
}
