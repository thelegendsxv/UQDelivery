package com.uqdelivery.service;

import com.uqdelivery.domain.Envio;
import com.uqdelivery.domain.EstadoEnvio;
import com.uqdelivery.domain.Role;
import com.uqdelivery.domain.Tarifa;
import com.uqdelivery.domain.User;
import com.uqdelivery.dto.CrearEnvioRequest;
import com.uqdelivery.dto.EnvioResponse;
import com.uqdelivery.exception.ResourceNotFoundException;
import com.uqdelivery.exception.TarifaNoDisponibleException;
import com.uqdelivery.repository.EnvioRepository;
import com.uqdelivery.repository.HistorialEstadoEnvioRepository;
import com.uqdelivery.repository.TarifaRepository;
import com.uqdelivery.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de HU-04 (reglas de creacion y consulta de envios) con Mockito, sin levantar Spring.
 * Usa un CotizacionService real (como en CotizacionServiceTest, con su TarifaRepository y DistanciaService
 * mockeados): asi se verifica que el precio y la distancia que guarda el envio vienen de ahi y no del cliente.
 */
class EnvioServiceTest {

    private static final String CLIENTE_EMAIL = "cliente@mail.com";

    private EnvioRepository envios;
    private HistorialEstadoEnvioRepository historial;
    private UserRepository usuarios;
    private TarifaRepository tarifas;
    private DistanciaService distancia;
    private EnvioService service;

    @BeforeEach
    void setUp() {
        envios = mock(EnvioRepository.class);
        historial = mock(HistorialEstadoEnvioRepository.class);
        usuarios = mock(UserRepository.class);
        tarifas = mock(TarifaRepository.class);
        distancia = mock(DistanciaService.class);
        CotizacionService cotizacionService = new CotizacionService(tarifas, distancia);
        service = new EnvioService(envios, historial, usuarios, cotizacionService);

        when(usuarios.findByEmail(CLIENTE_EMAIL)).thenReturn(Optional.of(cliente()));
        when(envios.existsByCodigoSeguimiento(any())).thenReturn(false);
        when(envios.save(any(Envio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tarifas.findFirstByActivaTrueOrderByIdAsc()).thenReturn(Optional.of(tarifaActiva()));
        when(distancia.calcularKm(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(bd("2.45"));
    }

    private static User cliente() {
        User u = new User("Cliente Prueba", CLIENTE_EMAIL, "hash", Role.CLIENTE_EMISOR);
        ReflectionTestUtils.setField(u, "id", 1L);
        return u;
    }

    private static Tarifa tarifaActiva() {
        return new Tarifa("Tarifa urbana estandar", bd("5000.00"), bd("1200.00"), bd("800.00"),
                bd("3000.00"), bd("5000.00"), bd("1.00"), true);
    }

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    private static CrearEnvioRequest req() {
        return new CrearEnvioRequest(bd("3"), bd("40"), bd("30"), bd("20"),
                "Calle 1 # 2-3", 4.5339, -75.6811, "Calle 9 # 10-11", 4.5450, -75.6620,
                "Juan Perez", "3001234567", "juan@mail.com", "Documentos", true, false);
    }

    //Criterio 1: crear con datos completos

    @Test
    void creaElEnvioConElPrecioYLaDistanciaDeLaCotizacion() {
        // peso facturable: real 3 kg vs volumetrico 40x30x20/5000=4.8 kg -> 4.8; precio = 5000 + 2.45x1200 (2940)
        // + 4.8x800 (3840) + 3000 (fragil) = 14780
        EnvioResponse res = service.crear(req(), CLIENTE_EMAIL);

        assertEquals(EstadoEnvio.PENDIENTE_ASIGNACION, res.estado());
        assertEquals(bd("2.45"), res.distanciaKm());
        assertEquals(bd("14780.00"), res.precioCotizado());
        assertTrue(res.codigoSeguimiento().startsWith("UQD-"));
        assertEquals(12, res.codigoSeguimiento().length()); // "UQD-" + 8 caracteres
        assertEquals("Juan Perez", res.receptorNombre());
        assertTrue(res.esFragil());
        assertFalse(res.esUrgente());
    }

    @Test
    void guardaElEnvioYElPrimerRegistroDeHistorial() {
        service.crear(req(), CLIENTE_EMAIL);

        verify(envios).save(any(Envio.class));
        verify(historial).save(argThat(h -> h.getEstadoAnterior() == null
                && h.getEstadoNuevo() == EstadoEnvio.PENDIENTE_ASIGNACION));
    }

    @Test
    void laDistanciaYElPrecioSeCalculanConLosMismosDatosDelEnvio() {
        // CrearEnvioRequest no tiene un campo de precio ni de distancia: no hay forma de que el cliente los envie.
        service.crear(req(), CLIENTE_EMAIL);
        verify(distancia).calcularKm(4.5339, -75.6811, 4.5450, -75.6620);
    }

    @Test
    void siNoHayTarifaActivaNoSeCreaNingunRegistro() {
        when(tarifas.findFirstByActivaTrueOrderByIdAsc()).thenReturn(Optional.empty());

        assertThrows(TarifaNoDisponibleException.class, () -> service.crear(req(), CLIENTE_EMAIL));
        verifyNoInteractions(envios);
        verifyNoInteractions(historial);
    }

    //Codigo de seguimiento unico

    @Test
    void reintentaElCodigoSiYaExiste() {
        when(envios.existsByCodigoSeguimiento(any())).thenReturn(true, true, false);

        service.crear(req(), CLIENTE_EMAIL);

        verify(envios, times(3)).existsByCodigoSeguimiento(any());
    }

    @Test
    void siNuncaEncuentraUnCodigoLibreLanzaExcepcion() {
        when(envios.existsByCodigoSeguimiento(any())).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.crear(req(), CLIENTE_EMAIL));
        verifyNoInteractions(historial);
    }

    //Consultas

    @Test
    void obtenerPorCodigoDelPropioCliente() {
        Envio envio = new Envio("UQD-ABC12345", "Origen", "Destino", cliente());
        when(envios.findByCodigoSeguimiento("UQD-ABC12345")).thenReturn(Optional.of(envio));

        assertEquals("UQD-ABC12345", service.obtenerPorCodigo("UQD-ABC12345", CLIENTE_EMAIL).codigoSeguimiento());
    }

    @Test
    void obtenerPorCodigoInexistenteLanza404() {
        when(envios.findByCodigoSeguimiento("UQD-NOPE0000")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.obtenerPorCodigo("UQD-NOPE0000", CLIENTE_EMAIL));
    }

    @Test
    void obtenerPorCodigoDeOtroClienteLanza404() {
        User otro = new User("Otro", "otro@mail.com", "hash", Role.CLIENTE_EMISOR);
        Envio envio = new Envio("UQD-AJENO001", "Origen", "Destino", otro);
        when(envios.findByCodigoSeguimiento("UQD-AJENO001")).thenReturn(Optional.of(envio));

        assertThrows(ResourceNotFoundException.class,
                () -> service.obtenerPorCodigo("UQD-AJENO001", CLIENTE_EMAIL));
    }

    @Test
    void misEnviosDelegaEnElRepositorioPorCorreo() {
        Envio envio = new Envio("UQD-MIO00001", "Origen", "Destino", cliente());
        when(envios.findByClienteEmailOrderByCreatedAtDesc(CLIENTE_EMAIL)).thenReturn(List.of(envio));

        List<EnvioResponse> res = service.misEnvios(CLIENTE_EMAIL);

        assertEquals(1, res.size());
        assertEquals("UQD-MIO00001", res.get(0).codigoSeguimiento());
    }
}
