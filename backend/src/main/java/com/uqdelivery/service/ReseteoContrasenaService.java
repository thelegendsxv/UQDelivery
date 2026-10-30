package com.uqdelivery.service;

import com.uqdelivery.domain.ReseteoContrasenaToken;
import com.uqdelivery.domain.User;
import com.uqdelivery.exception.ReseteoContrasenaException;
import com.uqdelivery.exception.TooManyAttemptsException;
import com.uqdelivery.repository.ReseteoContrasenaTokenRepository;
import com.uqdelivery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * HU-03 · Recuperacion de contrasena.
 *
 * forgot-password: genera un token aleatorio (SecureRandom, 32 bytes), guarda solo su hash SHA-256 con
 * expiracion de 30 min, invalida los tokens previos y envia el enlace. Responde siempre lo mismo.
 *
 * reset-password: valida el token (existe, no usado, no vencido), guarda la nueva contrasena con BCrypt
 * y marca el token como usado.
 */
@Service
public class ReseteoContrasenaService {

    static final String RATE_LIMIT_PREFIX = "reset:";
    private static final int TOKEN_BYTES = 32;

    private final UserRepository users;
    private final ReseteoContrasenaTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final ReseteoContrasenaEmailService email;
    private final LoginAttemptService attempts;
    private final String frontendUrl;
    private final Duration validez;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public ReseteoContrasenaService(UserRepository users, ReseteoContrasenaTokenRepository tokens, PasswordEncoder encoder,
                                    ReseteoContrasenaEmailService email, LoginAttemptService attempts,
                                    @Value("${app.password-reset.frontend-url}") String frontendUrl,
                                    @Value("${app.password-reset.expiration-minutes:30}") long expirationMinutes) {
        this(users, tokens, encoder, email, attempts, frontendUrl, expirationMinutes, Clock.systemUTC());
    }

    /** Constructor para pruebas: permite controlar el reloj. */
    ReseteoContrasenaService(UserRepository users, ReseteoContrasenaTokenRepository tokens, PasswordEncoder encoder,
                             ReseteoContrasenaEmailService email, LoginAttemptService attempts,
                             String frontendUrl, long expirationMinutes, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.email = email;
        this.attempts = attempts;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        this.validez = Duration.ofMinutes(expirationMinutes);
        this.clock = clock;
    }

    /** Criterio 1: con un correo registrado se inicia el proceso de recuperacion. */
    @Transactional
    public void solicitarRecuperacion(String emailIngresado) {
        String correo = emailIngresado.trim().toLowerCase(Locale.ROOT);
        String clave = RATE_LIMIT_PREFIX + correo;
        if (attempts.isLocked(clave)) {
            throw new TooManyAttemptsException("Demasiadas solicitudes de recuperacion. Intenta de nuevo en unos minutos");
        }
        attempts.recordFailure(clave); // cuenta cada solicitud, exista o no el correo

        User usuario = users.findByEmail(correo).orElse(null);
        if (usuario == null || !usuario.isActivo()) return; // misma respuesta: no se revela si el correo existe

        invalidarPendientes(usuario);
        String token = generarToken();
        tokens.save(new ReseteoContrasenaToken(usuario, hash(token), clock.instant().plus(validez)));
        email.enviarEnlace(usuario.getEmail(), usuario.getNombre(),
                frontendUrl + "/reset-password?token=" + token, validez.toMinutes());
    }

    /** Criterio 2: con un token valido se guarda la nueva contrasena y el usuario puede iniciar sesion. */
    @Transactional
    public void restablecer(String token, String nuevaPassword) {
        ReseteoContrasenaToken resetToken = tokens.findByTokenHash(hash(token))
                .orElseThrow(ReseteoContrasenaException::enlaceInvalido);
        User usuario = resetToken.getUsuario();
        if (!resetToken.esValido(clock.instant()) || !usuario.isActivo()) {
            throw ReseteoContrasenaException.enlaceInvalido();
        }
        if (encoder.matches(nuevaPassword, usuario.getPasswordHash())) {
            throw new ReseteoContrasenaException("La nueva contrasena debe ser diferente a la actual");
        }
        usuario.setPasswordHash(encoder.encode(nuevaPassword));
        users.save(usuario);
        resetToken.marcarUsado();
        tokens.save(resetToken);
        invalidarPendientes(usuario);
        attempts.reset(usuario.getEmail()); // si el login estaba bloqueado por intentos fallidos, se libera
    }

    //Auxiliares

    private void invalidarPendientes(User usuario) {
        tokens.findByUsuarioAndUsadoFalse(usuario).forEach(t -> {
            t.marcarUsado();
            tokens.save(t);
        });
    }

    private String generarToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); // 43 caracteres, seguro en URL
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
