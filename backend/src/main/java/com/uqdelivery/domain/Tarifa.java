package com.uqdelivery.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * HU-05 · Tarifa con la que se cotizan los envios. Valores en pesos colombianos (COP).
 * Regla: solo una tarifa puede estar activa a la vez (se controla en TarifaService).
 */
@Entity
@Table(name = "tarifas")
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(name = "tarifa_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal tarifaBase;

    @Column(name = "costo_por_km", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoPorKm;

    @Column(name = "costo_por_kg", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoPorKg;

    @Column(name = "recargo_fragil", nullable = false, precision = 12, scale = 2)
    private BigDecimal recargoFragil;

    @Column(name = "recargo_urgente", nullable = false, precision = 12, scale = 2)
    private BigDecimal recargoUrgente;

    /** Porcentaje sobre el valor declarado. Aun no se aplica: el envio no tiene valor declarado en el Sprint 1. */
    @Column(name = "porcentaje_seguro", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeSeguro;

    @Column(nullable = false)
    private boolean activa;

    protected Tarifa() { }

    public Tarifa(String nombre, BigDecimal tarifaBase, BigDecimal costoPorKm, BigDecimal costoPorKg,
                  BigDecimal recargoFragil, BigDecimal recargoUrgente, BigDecimal porcentajeSeguro, boolean activa) {
        this.nombre = nombre;
        this.tarifaBase = tarifaBase;
        this.costoPorKm = costoPorKm;
        this.costoPorKg = costoPorKg;
        this.recargoFragil = recargoFragil;
        this.recargoUrgente = recargoUrgente;
        this.porcentajeSeguro = porcentajeSeguro;
        this.activa = activa;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public BigDecimal getTarifaBase() { return tarifaBase; }
    public void setTarifaBase(BigDecimal tarifaBase) { this.tarifaBase = tarifaBase; }
    public BigDecimal getCostoPorKm() { return costoPorKm; }
    public void setCostoPorKm(BigDecimal costoPorKm) { this.costoPorKm = costoPorKm; }
    public BigDecimal getCostoPorKg() { return costoPorKg; }
    public void setCostoPorKg(BigDecimal costoPorKg) { this.costoPorKg = costoPorKg; }
    public BigDecimal getRecargoFragil() { return recargoFragil; }
    public void setRecargoFragil(BigDecimal recargoFragil) { this.recargoFragil = recargoFragil; }
    public BigDecimal getRecargoUrgente() { return recargoUrgente; }
    public void setRecargoUrgente(BigDecimal recargoUrgente) { this.recargoUrgente = recargoUrgente; }
    public BigDecimal getPorcentajeSeguro() { return porcentajeSeguro; }
    public void setPorcentajeSeguro(BigDecimal porcentajeSeguro) { this.porcentajeSeguro = porcentajeSeguro; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
