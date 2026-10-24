package com.uqdelivery.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entidad base de un envio, compartida por el equipo. Cada historia puede agregar
 * campos o metodos, pero conviene no cambiar los nombres existentes sin avisar al grupo.
 */
@Entity
@Table(name = "envios")
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_seguimiento", nullable = false, unique = true, length = 20)
    private String codigoSeguimiento;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "peso_kg")
    private Double pesoKg;

    @Column(name = "origen_direccion", nullable = false)
    private String origenDireccion;
    @Column(name = "origen_lat")
    private Double origenLat;
    @Column(name = "origen_lng")
    private Double origenLng;

    @Column(name = "destino_direccion", nullable = false)
    private String destinoDireccion;
    @Column(name = "destino_lat")
    private Double destinoLat;
    @Column(name = "destino_lng")
    private Double destinoLng;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEnvio estado = EstadoEnvio.PENDIENTE;

    /** Cliente emisor que solicito el envio. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private User cliente;

    /** Repartidor asignado (null mientras el envio no se asigne). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repartidor_id")
    private User repartidor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Envio() { }

    public Envio(String codigoSeguimiento, String origenDireccion, String destinoDireccion, User cliente) {
        this.codigoSeguimiento = codigoSeguimiento;
        this.origenDireccion = origenDireccion;
        this.destinoDireccion = destinoDireccion;
        this.cliente = cliente;
    }

    public Long getId() { return id; }
    public String getCodigoSeguimiento() { return codigoSeguimiento; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Double getPesoKg() { return pesoKg; }
    public void setPesoKg(Double pesoKg) { this.pesoKg = pesoKg; }
    public String getOrigenDireccion() { return origenDireccion; }
    public Double getOrigenLat() { return origenLat; }
    public Double getOrigenLng() { return origenLng; }
    public void setOrigenCoordenadas(Double lat, Double lng) { this.origenLat = lat; this.origenLng = lng; }
    public String getDestinoDireccion() { return destinoDireccion; }
    public Double getDestinoLat() { return destinoLat; }
    public Double getDestinoLng() { return destinoLng; }
    public void setDestinoCoordenadas(Double lat, Double lng) { this.destinoLat = lat; this.destinoLng = lng; }
    public EstadoEnvio getEstado() { return estado; }
    public void setEstado(EstadoEnvio estado) { this.estado = estado; }
    public User getCliente() { return cliente; }
    public User getRepartidor() { return repartidor; }
    public void setRepartidor(User repartidor) { this.repartidor = repartidor; }
    public Instant getCreatedAt() { return createdAt; }
}
