package com.uqdelivery.security;

import com.uqdelivery.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtMTIzNDU2";
    private final JwtService jwt = new JwtService(SECRET, 60);

    @Test
    void generatedTokenContainsSubjectAndRole() {
        Claims claims = jwt.parse(jwt.generateToken("a@b.com", Role.REPARTIDOR));
        assertEquals("a@b.com", claims.getSubject());
        assertEquals("REPARTIDOR", claims.get("role", String.class));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwt.generateToken("a@b.com", Role.CLIENTE_EMISOR);
        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThrows(JwtException.class, () -> jwt.parse(tampered));
    }

    @Test
    void tokenSignedWithOtherKeyIsRejected() {
        JwtService other = new JwtService(
                "b3RyYS1jbGF2ZS1kaWZlcmVudGUtb3RyYS1jbGF2ZS1kaWZlcmVudGUtMDk4NzY1", 60);
        String token = other.generateToken("a@b.com", Role.ADMINISTRADOR);
        assertThrows(JwtException.class, () -> jwt.parse(token));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService shortLived = new JwtService(SECRET, -1);
        String token = shortLived.generateToken("a@b.com", Role.CLIENTE_EMISOR);
        assertThrows(JwtException.class, () -> jwt.parse(token));
    }
}
