package com.uqdelivery.repository;

import com.uqdelivery.domain.Envio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EnvioRepository extends JpaRepository<Envio, Long> {
    Optional<Envio> findByCodigoSeguimiento(String codigoSeguimiento);
    boolean existsByCodigoSeguimiento(String codigoSeguimiento);

    //HU-04: historial propio del cliente, del mas reciente al mas antiguo
    List<Envio> findByClienteEmailOrderByCreatedAtDesc(String email);
}
