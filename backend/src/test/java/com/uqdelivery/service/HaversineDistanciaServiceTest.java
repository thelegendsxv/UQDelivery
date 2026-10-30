package com.uqdelivery.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas unitarias de HU-05: distancia en linea recta con Haversine (radio 6371 km). */
class HaversineDistanciaServiceTest {

    private final DistanciaService distancia = new HaversineDistanciaService();

    @Test
    void mismoPuntoDaDistanciaCero() {
        assertEquals(new BigDecimal("0.00"), distancia.calcularKm(4.5339, -75.6811, 4.5339, -75.6811));
    }

    @Test
    void unGradoDeLatitudSobreElMeridianoMideUnos111Km() {
        // 6371 x pi / 180 = 111.194...
        assertEquals(new BigDecimal("111.19"), distancia.calcularKm(0, 0, 1, 0));
    }

    @Test
    void puntosAntipodasDanMediaCircunferencia() {
        // pi x 6371 = 20015.086...
        assertEquals(new BigDecimal("20015.09"), distancia.calcularKm(0, 0, 0, 180));
    }

    @Test
    void esSimetricaYConDosDecimales() {
        BigDecimal ida = distancia.calcularKm(4.5339, -75.6811, 4.8133, -75.6961); // Armenia -> Pereira
        BigDecimal vuelta = distancia.calcularKm(4.8133, -75.6961, 4.5339, -75.6811);
        assertEquals(ida, vuelta);
        assertEquals(2, ida.scale());
        assertTrue(ida.compareTo(new BigDecimal("30")) > 0 && ida.compareTo(new BigDecimal("33")) < 0,
                "Armenia - Pereira en linea recta ronda 31 km, fue " + ida);
    }
}
