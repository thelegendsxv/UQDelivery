package com.uqdelivery.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entidad base de un envio, compartida por el equipo. Cada historia puede agregar
 * campos o metodos, pero conviene no cambiar los nombres existentes sin avisar al grupo.
 *
 * HU-04: agrega las dimensiones, los datos del receptor, las banderas de fragil/urgente y el
 * resultado de la cotizacion (distanciaKm, precioCotizado); ambos se calculan en el servidor con
 * CotizacionService (HU-05), nunca se reciben del cliente.
 */
@Entity
@Table(name = "envios")
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_seguimiento", nullable = false, unique = true, length = 20)
    private String codigoSeguimiento;

    /** Descripcion libre del contenido del paquete (descripcion_contenido en el DER). */
    @Column(length = 255)
    private String descripcion;

    @Column(name = "peso_kg", precision = 10, scale = 2)
    private BigDecimal pesoKg;

    @Column(name = "largo_cm", precision = 10, scale = 2)
    private BigDecimal largoCm;
    @Column(name = "ancho_cm", precision = 10, scale = 2)
    private BigDecimal anchoCm;
    @Column(name = "alto_cm", precision = 10, scale = 2)
    private BigDecimal altoCm;

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

    @Column(name = "receptor_nombre", length = 100)
    private String receptorNombre;
    @Column(name = "receptor_telefono", length = 20)
    private String receptorTelefono;
    @Column(name = "receptor_email", length = 150)
    private String receptorEmail;

    @Column(name = "es_fragil", nullable = false)
    private boolean esFragil = false;
    @Column(name = "es_urgente", nullable = false)
    private boolean esUrgente = false;

    /** Resultado de la cotizacion (HU-05) al momento de crear el envio: nunca lo decide el cliente. */
    @Column(name = "distancia_km", precision = 10, scale = 2)
    private BigDecimal distanciaKm;
    @Column(name = "precio_cotizado", precision = 12, scale = 2)
    private BigDecimal precioCotizado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEnvio estado = EstadoEnvio.PENDIENTE_ASIGNACION;

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

    @Column(name = "updated_at")
    private Instant updatedAt;

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
    public BigDecimal getPesoKg() { return pesoKg; }
    public void setPesoKg(BigDecimal pesoKg) { this.pesoKg = pesoKg; }

    public BigDecimal getLargoCm() { return largoCm; }
    public BigDecimal getAnchoCm() { return anchoCm; }
    public BigDecimal getAltoCm() { return altoCm; }
    public void setDimensiones(BigDecimal largoCm, BigDecimal anchoCm, BigDecimal altoCm) {
        this.largoCm = largoCm;
        this.anchoCm = anchoCm;
        this.altoCm = altoCm;
    }

    public String getOrigenDireccion() { return origenDireccion; }
    public Double getOrigenLat() { return origenLat; }
    public Double getOrigenLng() { return origenLng; }
    public void setOrigenCoordenadas(Double lat, Double lng) { this.origenLat = lat; this.origenLng = lng; }
    public String getDestinoDireccion() { return destinoDireccion; }
    public Double getDestinoLat() { return destinoLat; }
    public Double getDestinoLng() { return destinoLng; }
    public void setDestinoCoordenadas(Double lat, Double lng) { this.destinoLat = lat; this.destinoLng = lng; }

    public String getReceptorNombre() { return receptorNombre; }
    public String getReceptorTelefono() { return receptorTelefono; }
    public String getReceptorEmail() { return receptorEmail; }
    public void setReceptor(String nombre, String telefono, String email) {
        this.receptorNombre = nombre;
        this.receptorTelefono = telefono;
        this.receptorEmail = email;
    }

    public boolean isEsFragil() { return esFragil; }
    public void setEsFragil(boolean esFragil) { this.esFragil = esFragil; }
    public boolean isEsUrgente() { return esUrgente; }
    public void setEsUrgente(boolean esUrgente) { this.esUrgente = esUrgente; }

    public BigDecimal getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(BigDecimal distanciaKm) { this.distanciaKm = distanciaKm; }
    public BigDecimal getPrecioCotizado() { return precioCotizado; }
    public void setPrecioCotizado(BigDecimal precioCotizado) { this.precioCotizado = precioCotizado; }

    public EstadoEnvio getEstado() { return estado; }
    public void setEstado(EstadoEnvio estado) { this.estado = estado; }
    public User getCliente() { return cliente; }
    public User getRepartidor() { return repartidor; }
    public void setRepartidor(User repartidor) { this.repartidor = repartidor; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}
