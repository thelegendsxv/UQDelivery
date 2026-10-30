package com.uqdelivery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * HU-03 · Envia el correo con el enlace FRONTEND_URL/reset-password?token=...
 * Las credenciales SMTP solo vienen de variables de entorno (MAIL_*), nunca del repositorio.
 */
@Service
public class ReseteoContrasenaEmailService {

    private static final Logger log = LoggerFactory.getLogger(ReseteoContrasenaEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final boolean logLink;

    public ReseteoContrasenaEmailService(ObjectProvider<JavaMailSender> mailSender,
                                         @Value("${app.mail.from:no-reply@uqdelivery.com}") String from,
                                         @Value("${app.password-reset.log-link:false}") boolean logLink) {
        this.mailSender = mailSender;
        this.from = from;
        this.logLink = logLink;
    }

    @Async
    public void enviarEnlace(String email, String nombre, String enlace, long minutosValidez) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            if (logLink) {
                log.warn("MAIL_HOST no configurado. Enlace de recuperacion para {} (solo desarrollo): {}", email, enlace);
            } else {
                log.error("MAIL_HOST no configurado: no se pudo enviar el correo de recuperacion");
            }
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(email);
        msg.setSubject("UQDelivery - Recuperacion de contrasena");
        msg.setText(plantilla(nombre, enlace, minutosValidez));
        try {
            sender.send(msg);
        } catch (MailException e) {
            // No se propaga: el usuario siempre recibe la misma respuesta.
            log.error("No se pudo enviar el correo de recuperacion: {}", e.getMessage());
        }
    }

    static String plantilla(String nombre, String enlace, long minutosValidez) {
        return "Hola " + nombre + ",\n\n"
                + "Recibimos una solicitud para restablecer la contrasena de tu cuenta en UQDelivery.\n"
                + "Abre este enlace para crear una nueva contrasena. Vence en " + minutosValidez
                + " minutos y solo se puede usar una vez:\n\n"
                + enlace + "\n\n"
                + "Si no fuiste tu, ignora este mensaje: tu contrasena actual sigue funcionando.\n\n"
                + "Equipo UQDelivery";
    }
}
