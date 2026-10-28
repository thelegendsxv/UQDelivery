package com.uqdelivery.controller;

import com.uqdelivery.dto.AuthResponse;
import com.uqdelivery.dto.LoginRequest;
import com.uqdelivery.dto.RegisterRequest;
import com.uqdelivery.dto.UserResponse;
import com.uqdelivery.repository.UserRepository;
import com.uqdelivery.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;

    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    /** Devuelve el usuario de la sesion actual; sirve para verificar el token. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return users.findByEmail(authentication.getName())
                .map(u -> ResponseEntity.ok(UserResponse.from(u)))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
