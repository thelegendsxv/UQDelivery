package com.uqdelivery.config;

import com.uqdelivery.domain.Role;
import com.uqdelivery.domain.User;
import com.uqdelivery.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Crea el administrador inicial si no existe (el registro publico solo crea CLIENTE_EMISOR).
 * Credenciales por variables de entorno ADMIN_EMAIL y ADMIN_PASSWORD; si estan vacias no hace nada.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder,
                       @Value("${app.admin.email:}") String email,
                       @Value("${app.admin.password:}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password == null || password.isBlank()) {
            log.info("AdminSeeder: ADMIN_EMAIL/ADMIN_PASSWORD no definidos, no se crea administrador inicial");
            return;
        }
        if (users.existsByEmail(email)) return;
        users.save(new User("Administrador", email, encoder.encode(password), Role.ADMINISTRADOR));
        log.info("AdminSeeder: administrador inicial creado ({})", email);
    }
}