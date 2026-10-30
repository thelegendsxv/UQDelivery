package com.uqdelivery.controller;

import com.uqdelivery.dto.MessageResponse;
import com.uqdelivery.dto.ReseteoContrasenaRequest;
import com.uqdelivery.service.ReseteoContrasenaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU-03 · Recuperacion de contrasena. Endpoints publicos (contrato de API, seccion 7.1 de la guia). */
@RestController
@RequestMapping("/api/auth")
public class ReseteoContrasenaController {

    public static final String MENSAJE_SOLICITUD = "Si el correo existe, enviamos instrucciones para recuperar tu contrasena";
    public static final String MENSAJE_RESTABLECIDA = "Tu contrasena fue actualizada. Ya puedes iniciar sesion";

    private final ReseteoContrasenaService service;

    public ReseteoContrasenaController(ReseteoContrasenaService service) {
        this.service = service;
    }

    /** Siempre 200 con el mismo mensaje, exista o no el correo. */
    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody com.uqdelivery.dto.OlvidoContrasenaRequest req) {
        service.solicitarRecuperacion(req.email());
        return new MessageResponse(MENSAJE_SOLICITUD);
    }

    /** 200 si se cambio; 400 si el enlace no es valido, vencio o ya se uso, o si la contrasena es debil. */
    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ReseteoContrasenaRequest req) {
        service.restablecer(req.token(), req.password());
        return new MessageResponse(MENSAJE_RESTABLECIDA);
    }
}
