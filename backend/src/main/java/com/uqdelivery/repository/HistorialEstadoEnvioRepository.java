package com.uqdelivery.repository;

import com.uqdelivery.domain.HistorialEstadoEnvio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** HU-04 · Historial de estados de un envio, para trazabilidad (lo usaran HU-06, HU-12 y HU-14). */
public interface HistorialEstadoEnvioRepository extends JpaRepository<HistorialEstadoEnvio, Long> {
    List<HistorialEstadoEnvio> findByEnvioIdOrderByFechaAsc(Long envioId);
}
