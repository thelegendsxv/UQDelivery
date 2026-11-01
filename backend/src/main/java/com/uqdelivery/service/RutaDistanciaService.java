package com.uqdelivery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.OptionalInt;

/**
 * HU-05 · Distancia por las calles (como en Google Maps) usando el Routing API de TomTom.
 * Si no hay clave configurada o TomTom no responde, usa la linea recta (Haversine) para no tumbar la cotizacion.
 * Al ser @Primary, CotizacionService y EnvioService la usan sin cambiar nada.
 */
@Service
@Primary
public class RutaDistanciaService implements DistanciaService {

    private static final Logger log = LoggerFactory.getLogger(RutaDistanciaService.class);
    private static final BigDecimal METROS_POR_KM = new BigDecimal("1000");

    private final TomTomMapasClient mapas;
    private final HaversineDistanciaService respaldo;

    public RutaDistanciaService(TomTomMapasClient mapas, HaversineDistanciaService respaldo) {
        this.mapas = mapas;
        this.respaldo = respaldo;
    }

    @Override
    public BigDecimal calcularKm(double origenLat, double origenLng, double destinoLat, double destinoLng) {
        if (origenLat == destinoLat && origenLng == destinoLng) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP); // mismo punto: no se gasta una consulta
        }
        if (mapas.disponible()) {
            try {
                OptionalInt metros = mapas.distanciaMetros(origenLat, origenLng, destinoLat, destinoLng);
                if (metros.isPresent()) {
                    return BigDecimal.valueOf(metros.getAsInt()).divide(METROS_POR_KM, 2, RoundingMode.HALF_UP);
                }
                log.warn("TomTom no devolvio ruta; se usa la distancia en linea recta");
            } catch (RestClientException e) {
                log.warn("TomTom (ruta) fallo ({}); se usa la distancia en linea recta", TomTomMapasClient.resumen(e));
            }
        }
        return respaldo.calcularKm(origenLat, origenLng, destinoLat, destinoLng);
    }
}
