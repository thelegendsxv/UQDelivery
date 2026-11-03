package com.uqdelivery.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * HU-04 · Registro de cada cambio de estado de un envio (trazabilidad). El primer registro se crea
 * al solicitar el envio, con estadoAnterior en null. Lo usaran tambien HU-06, HU-12 y HU-14.
 */
@Entity
@Table(name = "historial_estado_envio")
public class HistorialEstadoEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "envio_id", nullable = false)
    private Envio envio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoEnvio estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoEnvio estadoNuevo;

    @Column(nullable = false, updatable = false)
    private Instant fecha = Instant.now();

    /** Quien provoco el cambio (el cliente al crear, el repartidor o el admin despues). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private User usuario;

    protected HistorialEstadoEnvio() { }

    public HistorialEstadoEnvio(Envio envio, EstadoEnvio estadoAnterior, EstadoEnvio estadoNuevo, User usuario) {
        this.envio = envio;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.usuario = usuario;
    }

    public Long getId() { return id; }
    public Envio getEnvio() { return envio; }
    public EstadoEnvio getEstadoAnterior() { return estadoAnterior; }
    public EstadoEnvio getEstadoNuevo() { return estadoNuevo; }
    public Instant getFecha() { return fecha; }
    public User getUsuario() { return usuario; }
}
