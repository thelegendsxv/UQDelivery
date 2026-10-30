package com.uqdelivery.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** HU-05 · Distancia en linea recta sobre la esfera terrestre (formula de Haversine). */
@Service
public class HaversineDistanciaService implements DistanciaService {

    static final double RADIO_TIERRA_KM = 6371.0;

    @Override
    public BigDecimal calcularKm(double origenLat, double origenLng, double destinoLat, double destinoLng) {
        double dLat = Math.toRadians(destinoLat - origenLat);
        double dLng = Math.toRadians(destinoLng - origenLng);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(origenLat)) * Math.cos(Math.toRadians(destinoLat))
                * Math.pow(Math.sin(dLng / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(RADIO_TIERRA_KM * c).setScale(2, RoundingMode.HALF_UP);
    }
}
