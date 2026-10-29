package com.uqdelivery.dto;

import com.uqdelivery.domain.Role;
import jakarta.validation.constraints.NotNull;

public record CambiarRolRequest(@NotNull(message = "El rol es obligatorio") Role role) {
}