package com.uqdelivery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Properties;

/**
 * HU-03 · Configuracion del modulo de recuperacion de contrasena.
 * - @EnableAsync: el correo se envia en segundo plano (la respuesta tarda lo mismo exista o no el correo).
 * - @EnableScheduling: limpieza programada de tokens vencidos.
 * - JavaMailSender a partir de las variables MAIL_* (Mailtrap o Gmail con contrasena de aplicacion).
 *   Si MAIL_HOST no esta definido no se crea el bean y, en desarrollo, el enlace se escribe en el log.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class ModuloContrasenaConfig {

    @Bean
    @ConditionalOnExpression("!'${app.mail.host:}'.isBlank()")
    public JavaMailSender javaMailSender(@Value("${app.mail.host}") String host,
                                         @Value("${app.mail.port:587}") int port,
                                         @Value("${app.mail.username:}") String username,
                                         @Value("${app.mail.password:}") String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(!username.isBlank()));
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        return sender;
    }
}
