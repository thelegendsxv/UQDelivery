package com.uqdelivery.dto;

import com.uqdelivery.domain.Role;
import com.uqdelivery.domain.User;

import java.time.Instant;

public record UsuarioResponse(Long id, String nombre, String email, String telefono, String direccion,
                              Role role, boolean activo, Instant createdAt) {
    public static UsuarioResponse from(User u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getTelefono(), u.getDireccion(),
                u.getRole(), u.isActivo(), u.getCreatedAt());
    }
}