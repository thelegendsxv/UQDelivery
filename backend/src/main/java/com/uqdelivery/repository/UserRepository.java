package com.uqdelivery.repository;

import com.uqdelivery.domain.Role;
import com.uqdelivery.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    //HU-02: evita dejar el sistema sin ningun administrador activo
    long countByRoleAndActivoTrue(Role role);
}