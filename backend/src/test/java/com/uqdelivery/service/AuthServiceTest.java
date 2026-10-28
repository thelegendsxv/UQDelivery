package com.uqdelivery.service;

import com.uqdelivery.domain.Role;
import com.uqdelivery.domain.User;
import com.uqdelivery.dto.AuthResponse;
import com.uqdelivery.dto.LoginRequest;
import com.uqdelivery.dto.RegisterRequest;
import com.uqdelivery.dto.UserResponse;
import com.uqdelivery.exception.DuplicateEmailException;
import com.uqdelivery.exception.InvalidCredentialsException;
import com.uqdelivery.exception.TooManyAttemptsException;
import com.uqdelivery.repository.UserRepository;
import com.uqdelivery.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtMTIzNDU2";

    private UserRepository repo;
    private PasswordEncoder encoder;
    private LoginAttemptService attempts;
    private JwtService jwt;
    private AuthService service;

    @BeforeEach
    void setUp() {
        repo = mock(UserRepository.class);
        encoder = new BCryptPasswordEncoder(4);
        attempts = new LoginAttemptService();
        jwt = new JwtService(SECRET, 60);
        service = new AuthService(repo, encoder, jwt, attempts);
    }

    private User storedUser(String email, String rawPassword) {
        return new User("Ana Perez", email, encoder.encode(rawPassword), Role.CLIENTE_EMISOR);
    }

    @Test
    void registerCreatesClienteEmisorWithHashedPasswordAndLowercaseEmail() {
        when(repo.existsByEmail("ana@mail.com")).thenReturn(false);
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse res = service.register(new RegisterRequest("  Ana Perez ", "Ana@Mail.com ", "Clave1234"));

        assertEquals("ana@mail.com", res.email());
        assertEquals("Ana Perez", res.nombre());
        assertEquals(Role.CLIENTE_EMISOR, res.role());
        verify(repo).save(argThat(u -> !u.getPasswordHash().equals("Clave1234")
                && encoder.matches("Clave1234", u.getPasswordHash())));
    }

    @Test
    void registerRejectsExistingEmail() {
        when(repo.existsByEmail("ana@mail.com")).thenReturn(true);
        assertThrows(DuplicateEmailException.class,
                () -> service.register(new RegisterRequest("Ana Perez", "ana@mail.com", "Clave1234")));
        verify(repo, never()).save(any());
    }

    @Test
    void registerMapsDatabaseUniqueViolationToDuplicate() {
        when(repo.existsByEmail(any())).thenReturn(false);
        when(repo.save(any())).thenThrow(new DataIntegrityViolationException("dup"));
        assertThrows(DuplicateEmailException.class,
                () -> service.register(new RegisterRequest("Ana Perez", "ana@mail.com", "Clave1234")));
    }

    @Test
    void loginWithValidCredentialsReturnsTokenAndRole() {
        when(repo.findByEmail("ana@mail.com")).thenReturn(Optional.of(storedUser("ana@mail.com", "Clave1234")));

        AuthResponse res = service.login(new LoginRequest("ANA@mail.com", "Clave1234"));

        assertEquals(Role.CLIENTE_EMISOR, res.role());
        assertEquals("ana@mail.com", jwt.parse(res.token()).getSubject());
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        when(repo.findByEmail("ana@mail.com")).thenReturn(Optional.of(storedUser("ana@mail.com", "Clave1234")));
        assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("ana@mail.com", "Incorrecta1")));
    }

    @Test
    void loginWithUnknownEmailGivesSameError() {
        when(repo.findByEmail("nadie@mail.com")).thenReturn(Optional.empty());
        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("nadie@mail.com", "Clave1234")));
        assertEquals(new InvalidCredentialsException().getMessage(), ex.getMessage());
    }

    @Test
    void accountIsLockedAfterTooManyFailures() {
        when(repo.findByEmail("ana@mail.com")).thenReturn(Optional.of(storedUser("ana@mail.com", "Clave1234")));
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES; i++) {
            assertThrows(InvalidCredentialsException.class,
                    () -> service.login(new LoginRequest("ana@mail.com", "Mala12345")));
        }
        // incluso con la clave correcta queda bloqueado
        assertThrows(TooManyAttemptsException.class,
                () -> service.login(new LoginRequest("ana@mail.com", "Clave1234")));
    }

    @Test
    void successfulLoginResetsFailureCounter() {
        when(repo.findByEmail("ana@mail.com")).thenReturn(Optional.of(storedUser("ana@mail.com", "Clave1234")));
        for (int i = 0; i < LoginAttemptService.MAX_FAILURES - 1; i++) {
            assertThrows(InvalidCredentialsException.class,
                    () -> service.login(new LoginRequest("ana@mail.com", "Mala12345")));
        }
        assertNotNull(service.login(new LoginRequest("ana@mail.com", "Clave1234")).token());
        assertThrows(InvalidCredentialsException.class,
                () -> service.login(new LoginRequest("ana@mail.com", "Mala12345")));
    }
}
