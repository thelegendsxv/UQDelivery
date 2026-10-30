package com.uqdelivery.repository;

import com.uqdelivery.domain.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** HU-05 · Consultas derivadas y JPQL parametrizado (sin concatenar SQL). */
public interface TarifaRepository extends JpaRepository<Tarifa, Long> {

    Optional<Tarifa> findFirstByActivaTrueOrderByIdAsc();

    List<Tarifa> findAllByOrderByIdAsc();

    /** Desactiva todas las tarifas menos la indicada, para que quede una sola activa. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Tarifa t set t.activa = false where t.activa = true and t.id <> :id")
    int desactivarOtras(@Param("id") Long id);
}
