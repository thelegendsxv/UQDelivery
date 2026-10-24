package com.uqdelivery.service;

import com.uqdelivery.domain.Role;
import com.uqdelivery.domain.User;
import com.uqdelivery.dto.AuthResponse;
import com.uqdelivery.dto.LoginRequest;
import com.uqdelivery.dto.RegisterRequest;
import com.uqdelivery.dto.UserResponse;
import com.uqdelivery.exception.DuplicateEmailException;
import com.uqdelivery.exception.InvalidCredentialsException;
import com.uqdelivery.repository.UserRepository;
import com.uqdelivery.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    /** Hash falso para que el tiempo de respuesta sea similar cuando el correo no existe. */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.dummyHash = encoder.encode("contrasena-falsa-Uq1");
    }

    /**
     * El registro publico SIEMPRE crea un CLIENTE_EMISOR. Los repartidores y administradores
     * los gestiona un administrador (HU-02); asi nadie puede auto-asignarse un rol privilegiado.
     */
    public UserResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (users.existsByEmail(email)) throw new DuplicateEmailException();
        User user = new User(req.nombre().trim(), email, encoder.encode(req.password()), Role.CLIENTE_EMISOR);
        try {
            return UserResponse.from(users.save(user));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException();
        }
    }

    public AuthResponse login(LoginRequest req) {
        String email = normalize(req.email());
        User user = users.findByEmail(email).orElse(null);
        boolean matches = encoder.matches(req.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !matches) throw new InvalidCredentialsException();
        return new AuthResponse(jwt.generateToken(user.getEmail(), user.getRole()),
                user.getNombre(), user.getEmail(), user.getRole());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
