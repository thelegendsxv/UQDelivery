package com.uqdelivery.service;

import com.uqdelivery.domain.Role;
import com.uqdelivery.domain.User;
import com.uqdelivery.exception.ReseteoContrasenaException;
import com.uqdelivery.exception.TooManyAttemptsException;
import com.uqdelivery.domain.ReseteoContrasenaToken;
import com.uqdelivery.repository.ReseteoContrasenaTokenRepository;
import com.uqdelivery.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReseteoContrasenaServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-29T15:00:00Z");
    private static final String EMAIL = "ana@mail.com";
    private static final String TOKEN = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-abcde"; // 43 caracteres

    private UserRepository users;
    private ReseteoContrasenaTokenRepository tokens;
    private PasswordEncoder encoder;
    private ReseteoContrasenaEmailService email;
    private LoginAttemptService attempts;
    private ReseteoContrasenaService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        tokens = mock(ReseteoContrasenaTokenRepository.class);
        encoder = new BCryptPasswordEncoder(4);
        email = mock(ReseteoContrasenaEmailService.class);
        attempts = new LoginAttemptService();
        service = new ReseteoContrasenaService(users, tokens, encoder, email, attempts,
                "http://localhost:5173/", 30, Clock.fixed(NOW, ZoneOffset.UTC));
        when(tokens.save(any(ReseteoContrasenaToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokens.findByUsuarioAndUsadoFalse(any())).thenReturn(List.of());
    }

    private User usuario(String passwordActual) {
        return new User("Ana Perez", EMAIL, encoder.encode(passwordActual), Role.CLIENTE_EMISOR);
    }

    private ReseteoContrasenaToken tokenGuardado(User user, Instant expira) {
        ReseteoContrasenaToken t = new ReseteoContrasenaToken(user, ReseteoContrasenaService.hash(TOKEN), expira);
        when(tokens.findByTokenHash(ReseteoContrasenaService.hash(TOKEN))).thenReturn(Optional.of(t));
        return t;
    }

    // ---------- forgot-password (criterio 1) ----------

    @Test
    void correoRegistradoGeneraTokenHasheadoYEnviaEnlace() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(usuario("Clave1234")));

        service.solicitarRecuperacion("  Ana@Mail.com ");

        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(email).enviarEnlace(eq(EMAIL), eq("Ana Perez"), enlace.capture(), eq(30L));
        assertTrue(enlace.getValue().startsWith("http://localhost:5173/reset-password?token="));
        String token = enlace.getValue().substring(enlace.getValue().indexOf("token=") + 6);
        assertEquals(43, token.length(), "32 bytes en Base64 URL");

        ArgumentCaptor<ReseteoContrasenaToken> guardado = ArgumentCaptor.forClass(ReseteoContrasenaToken.class);
        verify(tokens).save(guardado.capture());
        assertNotEquals(token, guardado.getValue().getTokenHash(), "no se guarda el token en claro");
        assertEquals(ReseteoContrasenaService.hash(token), guardado.getValue().getTokenHash());
        assertEquals(NOW.plus(Duration.ofMinutes(30)), guardado.getValue().getFechaExpiracion());
        assertFalse(guardado.getValue().isUsado());
    }

    @Test
    void correoInexistenteNoGuardaNiEnviaYNoLanzaError() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.solicitarRecuperacion("nadie@mail.com"));

        verify(tokens, never()).save(any());
        verifyNoInteractions(email);
    }

    @Test
    void cuentaDesactivadaNoRecibeEnlace() {
        User user = usuario("Clave1234");
        user.setActivo(false);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.solicitarRecuperacion(EMAIL);

        verifyNoInteractions(email);
        verify(tokens, never()).save(any());
    }

    @Test
    void nuevaSolicitudInvalidaTokensPrevios() {
        User user = usuario("Clave1234");
        ReseteoContrasenaToken anterior = new ReseteoContrasenaToken(user, "hash-anterior", NOW.plusSeconds(600));
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokens.findByUsuarioAndUsadoFalse(user)).thenReturn(List.of(anterior));

        service.solicitarRecuperacion(EMAIL);

        assertTrue(anterior.isUsado());
        assertFalse(anterior.esValido(NOW));
    }

    @Test
    void masDeCincoSolicitudesSonBloqueadasSinAfectarElLogin() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) service.solicitarRecuperacion(EMAIL);

        assertThrows(TooManyAttemptsException.class, () -> service.solicitarRecuperacion(EMAIL));
        assertFalse(attempts.isLocked(EMAIL));
    }

    // ---------- reset-password (criterio 2) ----------

    @Test
    void tokenValidoActualizaContrasenaConBCryptYMarcaUsado() {
        User user = usuario("Clave1234");
        ReseteoContrasenaToken t = tokenGuardado(user, NOW.plusSeconds(600));

        service.restablecer(TOKEN, "NuevaClave99");

        assertTrue(encoder.matches("NuevaClave99", user.getPasswordHash()));
        assertFalse(encoder.matches("Clave1234", user.getPasswordHash()));
        assertTrue(t.isUsado());
        verify(users).save(user);
    }

    @Test
    void restablecerDesbloqueaElLogin() {
        User user = usuario("Clave1234");
        tokenGuardado(user, NOW.plusSeconds(600));
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) attempts.recordFailure(EMAIL);

        service.restablecer(TOKEN, "NuevaClave99");

        assertFalse(attempts.isLocked(EMAIL));
    }

    @Test
    void tokenYaUsadoEsRechazado() {
        User user = usuario("Clave1234");
        tokenGuardado(user, NOW.plusSeconds(600));
        service.restablecer(TOKEN, "NuevaClave99");

        assertThrows(ReseteoContrasenaException.class, () -> service.restablecer(TOKEN, "OtraClave77"));
    }

    @Test
    void tokenVencidoEsRechazado() {
        tokenGuardado(usuario("Clave1234"), NOW.minusSeconds(1));

        ReseteoContrasenaException ex = assertThrows(ReseteoContrasenaException.class,
                () -> service.restablecer(TOKEN, "NuevaClave99"));
        assertTrue(ex.getMessage().contains("no es valido o ya expiro"));
        verify(users, never()).save(any());
    }

    @Test
    void tokenInexistenteEsRechazado() {
        when(tokens.findByTokenHash(any())).thenReturn(Optional.empty());
        assertThrows(ReseteoContrasenaException.class, () -> service.restablecer(TOKEN, "NuevaClave99"));
    }

    @Test
    void cuentaDesactivadaNoPuedeUsarUnTokenEmitido() {
        User user = usuario("Clave1234");
        tokenGuardado(user, NOW.plusSeconds(600));
        user.setActivo(false);

        assertThrows(ReseteoContrasenaException.class, () -> service.restablecer(TOKEN, "NuevaClave99"));
    }

    @Test
    void laNuevaContrasenaDebeSerDistintaALaActual() {
        User user = usuario("Clave1234");
        ReseteoContrasenaToken t = tokenGuardado(user, NOW.plusSeconds(600));

        assertThrows(ReseteoContrasenaException.class, () -> service.restablecer(TOKEN, "Clave1234"));
        assertFalse(t.isUsado(), "el token sigue disponible para intentar con otra contrasena");
    }

    @Test
    void hashEsSha256Hexadecimal() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                ReseteoContrasenaService.hash("abc"));
    }
}
