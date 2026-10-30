package com.uqdelivery.repository;

import com.uqdelivery.domain.User;
import com.uqdelivery.domain.ReseteoContrasenaToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** HU-03 */
public interface ReseteoContrasenaTokenRepository extends JpaRepository<ReseteoContrasenaToken, Long> {

    Optional<ReseteoContrasenaToken> findByTokenHash(String tokenHash);

    /** Tokens aun no usados de un usuario, para invalidarlos al pedir uno nuevo o al cambiar la contrasena. */
    List<ReseteoContrasenaToken> findByUsuarioAndUsadoFalse(User usuario);

    /** Limpieza programada: borra los tokens vencidos (consulta parametrizada). */
    @Modifying
    @Query("delete from ReseteoContrasenaToken t where t.fechaExpiracion < :limite")
    int borrarVencidos(@Param("limite") Instant limite);
}
