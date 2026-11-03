package com.uqdelivery.dto;

import com.uqdelivery.domain.Envio;
import com.uqdelivery.domain.EstadoEnvio;

import java.math.BigDecimal;
import java.time.Instant;

/** HU-04 · Datos de un envio para el cliente. El controlador nunca devuelve la entidad directamente. */
public record EnvioResponse(
        Long id,
        String codigoSeguimiento,
        EstadoEnvio estado,
        BigDecimal pesoKg,
        BigDecimal largoCm,
        BigDecimal anchoCm,
        BigDecimal altoCm,
        String origenDireccion,
        Double origenLat,
        Double origenLng,
        String destinoDireccion,
        Double destinoLat,
        Double destinoLng,
        String receptorNombre,
        String receptorTelefono,
        String receptorEmail,
        String descripcion,
        boolean esFragil,
        boolean esUrgente,
        BigDecimal distanciaKm,
        BigDecimal precioCotizado,
        Instant createdAt) {

    public static EnvioResponse from(Envio e) {
        return new EnvioResponse(e.getId(), e.getCodigoSeguimiento(), e.getEstado(), e.getPesoKg(), e.getLargoCm(),
                e.getAnchoCm(), e.getAltoCm(), e.getOrigenDireccion(), e.getOrigenLat(), e.getOrigenLng(),
                e.getDestinoDireccion(), e.getDestinoLat(), e.getDestinoLng(), e.getReceptorNombre(),
                e.getReceptorTelefono(), e.getReceptorEmail(), e.getDescripcion(), e.isEsFragil(), e.isEsUrgente(),
                e.getDistanciaKm(), e.getPrecioCotizado(), e.getCreatedAt());
    }
}
