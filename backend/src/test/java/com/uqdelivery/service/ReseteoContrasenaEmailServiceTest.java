package com.uqdelivery.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU-03 · Mock de JavaMailSender. */
class ReseteoContrasenaEmailServiceTest {

    @SuppressWarnings("unchecked")
    private static ObjectProvider<JavaMailSender> provider(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(sender);
        return p;
    }

    @Test
    void enviaCorreoConElEnlaceYLaVigencia() {
        JavaMailSender sender = mock(JavaMailSender.class);
        var service = new ReseteoContrasenaEmailService(provider(sender), "no-reply@uqdelivery.com", false);

        service.enviarEnlace("ana@mail.com", "Ana", "http://front/reset-password?token=abc", 30);

        ArgumentCaptor<SimpleMailMessage> msg = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(msg.capture());
        assertArrayEquals(new String[]{"ana@mail.com"}, msg.getValue().getTo());
        assertEquals("no-reply@uqdelivery.com", msg.getValue().getFrom());
        assertTrue(msg.getValue().getText().contains("http://front/reset-password?token=abc"));
        assertTrue(msg.getValue().getText().contains("30 minutos"));
    }

    @Test
    void sinSmtpConfiguradoNoFalla() {
        assertDoesNotThrow(() -> new ReseteoContrasenaEmailService(provider(null), "x@uq.com", true)
                .enviarEnlace("ana@mail.com", "Ana", "http://x", 30));
        assertDoesNotThrow(() -> new ReseteoContrasenaEmailService(provider(null), "x@uq.com", false)
                .enviarEnlace("ana@mail.com", "Ana", "http://x", 30));
    }

    @Test
    void unErrorDelSmtpNoSePropaga() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("caido")).when(sender).send(any(SimpleMailMessage.class));
        var service = new ReseteoContrasenaEmailService(provider(sender), "x@uq.com", false);

        assertDoesNotThrow(() -> service.enviarEnlace("ana@mail.com", "Ana", "http://x", 30));
    }
}
