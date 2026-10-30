package com.uqdelivery.service;

import com.uqdelivery.domain.Envio;
import com.uqdelivery.domain.EstadoEnvio;
import com.uqdelivery.domain.HistorialEstadoEnvio;
import com.uqdelivery.domain.User;
import com.uqdelivery.dto.CotizacionRequest;
import com.uqdelivery.dto.CotizacionResponse;
import com.uqdelivery.dto.CrearEnvioRequest;
import com.uqdelivery.dto.EnvioResponse;
import com.uqdelivery.exception.ResourceNotFoundException;
import com.uqdelivery.repository.EnvioRepository;
import com.uqdelivery.repository.HistorialEstadoEnvioRepository;
import com.uqdelivery.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

/**
 * HU-04 · Creacion y consulta de solicitudes de envio.
 * El precio y la distancia nunca llegan del cliente: se calculan aqui con CotizacionService (HU-05),
 * con los mismos datos del paquete y las coordenadas, para que no se puedan manipular desde el cliente.
 */
@Service
public class EnvioService {

    // Sin O/0/I/1 para que el codigo se lea y se dicte facil por telefono.
    static final String ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final int LARGO_CODIGO = 8;
    static final int INTENTOS_MAXIMOS = 10;

    private final EnvioRepository envios;
    private final HistorialEstadoEnvioRepository historial;
    private final UserRepository usuarios;
    private final CotizacionService cotizacionService;
    private final SecureRandom random = new SecureRandom();

    public EnvioService(EnvioRepository envios, HistorialEstadoEnvioRepository historial,
                        UserRepository usuarios, CotizacionService cotizacionService) {
        this.envios = envios;
        this.historial = historial;
        this.usuarios = usuarios;
        this.cotizacionService = cotizacionService;
    }

    /** Criterio de aceptacion 2: si la validacion del DTO falla, este metodo nunca se llama (no queda registro parcial). */
    @Transactional
    public EnvioResponse crear(CrearEnvioRequest req, String emailCliente) {
        User cliente = buscarCliente(emailCliente);

        CotizacionResponse cotizacion = cotizacionService.cotizar(new CotizacionRequest(
                req.pesoKg(), req.largoCm(), req.anchoCm(), req.altoCm(),
                req.origenLat(), req.origenLng(), req.destinoLat(), req.destinoLng(),
                req.esFragil(), req.esUrgente()));

        Envio envio = new Envio(generarCodigo(), req.origenDireccion().trim(), req.destinoDireccion().trim(), cliente);
        envio.setOrigenCoordenadas(req.origenLat(), req.origenLng());
        envio.setDestinoCoordenadas(req.destinoLat(), req.destinoLng());
        envio.setPesoKg(req.pesoKg());
        envio.setDimensiones(req.largoCm(), req.anchoCm(), req.altoCm());
        envio.setReceptor(req.receptorNombre().trim(), req.receptorTelefono().trim(), vacioANull(req.receptorEmail()));
        envio.setDescripcion(vacioANull(req.descripcion()));
        envio.setEsFragil(req.fragil());
        envio.setEsUrgente(req.urgente());
        envio.setDistanciaKm(cotizacion.distanciaKm());
        envio.setPrecioCotizado(cotizacion.total());
        envio.setEstado(EstadoEnvio.PENDIENTE_ASIGNACION);

        envios.save(envio);
        historial.save(new HistorialEstadoEnvio(envio, null, EstadoEnvio.PENDIENTE_ASIGNACION, cliente));

        return EnvioResponse.from(envio);
    }

    @Transactional(readOnly = true)
    public EnvioResponse obtenerPorCodigo(String codigo, String emailSolicitante) {
        Envio envio = envios.findByCodigoSeguimiento(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Envío no encontrado"));
        // No revela si el codigo existe cuando es de otro cliente (mismo criterio de HU-03).
        if (!envio.getCliente().getEmail().equalsIgnoreCase(emailSolicitante)) {
            throw new ResourceNotFoundException("Envío no encontrado");
        }
        return EnvioResponse.from(envio);
    }

    @Transactional(readOnly = true)
    public List<EnvioResponse> misEnvios(String emailCliente) {
        return envios.findByClienteEmailOrderByCreatedAtDesc(emailCliente).stream().map(EnvioResponse::from).toList();
    }

    private User buscarCliente(String email) {
        return usuarios.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private String generarCodigo() {
        for (int intento = 0; intento < INTENTOS_MAXIMOS; intento++) {
            String codigo = "UQD-" + aleatorio();
            if (!envios.existsByCodigoSeguimiento(codigo)) return codigo;
        }
        throw new IllegalStateException("No se pudo generar un código de seguimiento único");
    }

    private String aleatorio() {
        StringBuilder sb = new StringBuilder(LARGO_CODIGO);
        for (int i = 0; i < LARGO_CODIGO; i++) {
            sb.append(ALFABETO_CODIGO.charAt(random.nextInt(ALFABETO_CODIGO.length())));
        }
        return sb.toString();
    }

    private static String vacioANull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
