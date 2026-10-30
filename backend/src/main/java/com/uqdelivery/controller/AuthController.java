package com.uqdelivery.controller;

import com.uqdelivery.dto.AuthResponse;
import com.uqdelivery.dto.LoginRequest;
import com.uqdelivery.dto.RegisterRequest;
import com.uqdelivery.dto.UserResponse;
import com.uqdelivery.repository.UserRepository;
import com.uqdelivery.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro e inicio de sesión (HU-01)")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;

    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @Operation(summary = "Registra un cliente emisor",
               description = "Crea la cuenta con rol CLIENTE_EMISOR. No permite elegir otro rol.")
    @ApiResponse(responseCode = "201", description = "Cuenta creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos (devuelve los campos con error)")
    @ApiResponse(responseCode = "409", description = "Ya existe una cuenta con ese correo")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @Operation(summary = "Inicia sesión",
               description = "Devuelve un token JWT y los datos básicos del usuario. "
                       + "El mensaje de error es genérico para no revelar si el correo existe.")
    @ApiResponse(responseCode = "200", description = "Sesión iniciada")
    @ApiResponse(responseCode = "400", description = "Faltan datos")
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos")
    @ApiResponse(responseCode = "429", description = "Demasiados intentos fallidos; cuenta bloqueada temporalmente")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    /** Devuelve el usuario de la sesion actual; sirve para verificar el token. */
    @Operation(summary = "Usuario de la sesión actual",
               description = "Requiere el token JWT en el encabezado Authorization.")
    @ApiResponse(responseCode = "200", description = "Datos del usuario autenticado")
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        return users.findByEmail(authentication.getName())
                .map(u -> ResponseEntity.ok(UserResponse.from(u)))
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
