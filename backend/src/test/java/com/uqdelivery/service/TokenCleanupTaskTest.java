package com.uqdelivery.service;

import com.uqdelivery.repository.ReseteoContrasenaTokenRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TokenCleanupTaskTest {

    @Test
    void borraLosTokensVencidosHastaAhora() {
        ReseteoContrasenaTokenRepository repo = mock(ReseteoContrasenaTokenRepository.class);
        when(repo.borrarVencidos(any())).thenReturn(3);
        Instant antes = Instant.now();

        new TokenCleanupTask(repo).borrarTokensVencidos();

        verify(repo).borrarVencidos(argThat(limite -> !limite.isBefore(antes)));
    }
}
