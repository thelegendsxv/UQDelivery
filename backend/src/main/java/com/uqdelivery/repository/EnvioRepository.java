package com.uqdelivery.repository;

import com.uqdelivery.domain.Envio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EnvioRepository extends JpaRepository<Envio, Long> {
    Optional<Envio> findByCodigoSeguimiento(String codigoSeguimiento);
}
