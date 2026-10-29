package com.uqdelivery.controller;

import com.uqdelivery.domain.Role;
import com.uqdelivery.dto.*;
import com.uqdelivery.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** HU-02 · Gestion de perfiles y roles */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    /** Lista paginada con filtros opcionales */
    @GetMapping
    public PageResponse<UsuarioResponse> listar(@RequestParam(required = false) Role rol,
                                                @RequestParam(required = false) Boolean activo,
                                                @RequestParam(required = false) String q,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return service.listar(rol, activo, q, page, size);
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /** El administrador crea cuentas con rol */
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    /** Criterio de aceptacion 1: el administrador asigna o modifica el rol de un usuario. */
    @PutMapping("/{id}/rol")
    public UsuarioResponse cambiarRol(@PathVariable Long id, @Valid @RequestBody CambiarRolRequest req,
                                      Authentication auth) {
        return service.cambiarRol(id, req.role(), auth.getName());
    }

    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest req,
                                         Authentication auth) {
        return service.cambiarEstado(id, req.activo(), auth.getName());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UsuarioResponse miPerfil(Authentication auth) {
        return service.perfil(auth.getName());
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UsuarioResponse actualizarMiPerfil(@Valid @RequestBody ActualizarPerfilRequest req, Authentication auth) {
        return service.actualizarPerfil(auth.getName(), req);
    }
}