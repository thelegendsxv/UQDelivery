package com.uqdelivery.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * HU-03 · Token de recuperacion de contrasena (tabla password_reset_tokens, ver V2__password_reset_tokens.sql).
 * Solo se guarda el hash SHA-256 del token: quien lea la BD no puede usar los enlaces enviados.
 * Un token es de un solo uso y vence a los 30 minutos.
 */
@Entity
@Table(name = "password_reset_tokens")
public class ReseteoContrasenaToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;

    @Column(nullable = false)
    private boolean usado = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion = Instant.now();

    protected ReseteoContrasenaToken() { }

    public ReseteoContrasenaToken(User usuario, String tokenHash, Instant fechaExpiracion) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.fechaExpiracion = fechaExpiracion;
    }

    /** Valido = no usado y no vencido. */
    public boolean esValido(Instant ahora) {
        return !usado && ahora.isBefore(fechaExpiracion);
    }

    public void marcarUsado() { this.usado = true; }

    public Long getId() { return id; }
    public User getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public Instant getFechaExpiracion() { return fechaExpiracion; }
    public boolean isUsado() { return usado; }
    public Instant getFechaCreacion() { return fechaCreacion; }
}
