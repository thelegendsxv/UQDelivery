package com.uqdelivery.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuntosConocidosTest {

    @Test
    void reconoceLaUniversidadConTildesMayusculasOTextoIncompleto() {
        assertEquals("uq-ingenieria", PuntosConocidos.buscar("Universidad del Quindío").get(0).id());
        assertEquals("uq-ingenieria", PuntosConocidos.buscar("UNIVERSIDAD DEL QUINDIO").get(0).id());
        assertEquals("uq-ingenieria", PuntosConocidos.buscar("universidad del qui").get(0).id());
        assertEquals("uq-ingenieria", PuntosConocidos.buscar("uniquindio").get(0).id());
    }

    @Test
    void otrosTextosNoCoinciden() {
        assertTrue(PuntosConocidos.buscar("Granada").isEmpty());
        assertTrue(PuntosConocidos.buscar("Universidad del Valle").isEmpty());
        assertTrue(PuntosConocidos.buscar("uq").isEmpty());
        assertTrue(PuntosConocidos.buscar(null).isEmpty());
    }

    @Test
    void ningunPuntoSaleConCoordenadasEnCero() {
        PuntosConocidos.buscar("Universidad del Quindio")
                .forEach(l -> assertTrue(l.lat() != 0 && l.lng() != 0, l.id() + " sin coordenadas"));
    }
}
