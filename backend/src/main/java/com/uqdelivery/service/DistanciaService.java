package com.uqdelivery.service;

import java.math.BigDecimal;

/**
 * HU-05 · Calcula la distancia entre origen y destino en km (2 decimales).
 * En el Sprint 1 la implementa HaversineDistanciaService (linea recta). En el Sprint 2 se puede
 * registrar otra implementacion con la distancia por ruta de la API de mapas marcandola @Primary.
 */
public interface DistanciaService {

    BigDecimal calcularKm(double origenLat, double origenLng, double destinoLat, double destinoLng);
}
