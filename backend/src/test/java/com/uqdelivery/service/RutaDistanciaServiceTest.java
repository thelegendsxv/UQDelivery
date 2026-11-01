package com.uqdelivery.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RutaDistanciaServiceTest {

    // Universidad del Quindio -> Granada (Armenia), aprox. 3 km en linea recta
    private static final double O_LAT = 4.5533, O_LNG = -75.6606, D_LAT = 4.5400, D_LNG = -75.6700;

    private final HaversineDistanciaService haversine = new HaversineDistanciaService();
    private TomTomMapasClient mapas;
    private RutaDistanciaService servicio;

    @BeforeEach
    void preparar() {
        mapas = mock(TomTomMapasClient.class);
        servicio = new RutaDistanciaService(mapas, haversine);
    }

    @Test
    void conClaveUsaLosMetrosDeLaRutaEnKm() {
        when(mapas.disponible()).thenReturn(true);
        when(mapas.distanciaMetros(O_LAT, O_LNG, D_LAT, D_LNG)).thenReturn(OptionalInt.of(1890));
        assertEquals(new BigDecimal("1.89"), servicio.calcularKm(O_LAT, O_LNG, D_LAT, D_LNG));
    }

    @Test
    void sinClaveUsaLaLineaRecta() {
        when(mapas.disponible()).thenReturn(false);
        assertEquals(haversine.calcularKm(O_LAT, O_LNG, D_LAT, D_LNG), servicio.calcularKm(O_LAT, O_LNG, D_LAT, D_LNG));
        verify(mapas, never()).distanciaMetros(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    void siTomTomFallaUsaLaLineaRecta() {
        when(mapas.disponible()).thenReturn(true);
        when(mapas.distanciaMetros(O_LAT, O_LNG, D_LAT, D_LNG)).thenThrow(new RestClientException("sin red"));
        assertEquals(haversine.calcularKm(O_LAT, O_LNG, D_LAT, D_LNG), servicio.calcularKm(O_LAT, O_LNG, D_LAT, D_LNG));
    }

    @Test
    void siTomTomNoDevuelveRutaUsaLaLineaRecta() {
        when(mapas.disponible()).thenReturn(true);
        when(mapas.distanciaMetros(O_LAT, O_LNG, D_LAT, D_LNG)).thenReturn(OptionalInt.empty());
        assertEquals(haversine.calcularKm(O_LAT, O_LNG, D_LAT, D_LNG), servicio.calcularKm(O_LAT, O_LNG, D_LAT, D_LNG));
    }

    @Test
    void mismoPuntoEsCeroYNoConsultaAlProveedor() {
        when(mapas.disponible()).thenReturn(true);
        assertEquals(new BigDecimal("0.00"), servicio.calcularKm(O_LAT, O_LNG, O_LAT, O_LNG));
        verify(mapas, never()).distanciaMetros(anyDouble(), anyDouble(), anyDouble(), anyDouble());
    }
}
