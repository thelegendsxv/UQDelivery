package com.uqdelivery.dto;

import com.uqdelivery.domain.Role;

public record AuthResponse(String token, String nombre, String email, Role role) {
}
