package com.uqdelivery.service;

import com.uqdelivery.repository.ReseteoContrasenaTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** HU-03 · Borra los tokens vencidos todos los dias (por defecto a las 3:00 a. m.). */
@Component
public class TokenCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupTask.class);

    private final ReseteoContrasenaTokenRepository tokens;

    public TokenCleanupTask(ReseteoContrasenaTokenRepository tokens) {
        this.tokens = tokens;
    }

    @Scheduled(cron = "${app.password-reset.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void borrarTokensVencidos() {
        int borrados = tokens.borrarVencidos(Instant.now());
        if (borrados > 0) log.info("Limpieza HU-03: {} tokens de recuperacion vencidos eliminados", borrados);
    }
}
