package com.uqdelivery.dto;

import com.uqdelivery.domain.Role;
import com.uqdelivery.domain.User;

public record UserResponse(Long id, String nombre, String email, Role role) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRole());
    }
}
