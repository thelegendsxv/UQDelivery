package com.uqdelivery.repository;

import com.uqdelivery.domain.Envio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;

public interface EnvioRepository extends JpaRepository<Envio, Long>, JpaSpecificationExecutor<Envio> {
    Optional<Envio> findByCodigoSeguimiento(String codigoSeguimiento);
    boolean existsByCodigoSeguimiento(String codigoSeguimiento);

    //HU-04: historial propio del cliente, del mas reciente al mas antiguo
    List<Envio> findByClienteEmailOrderByCreatedAtDesc(String email);
    //HU-14: el historial con filtros se arma con Specification (ver EnvioService.misEnvios)
}
