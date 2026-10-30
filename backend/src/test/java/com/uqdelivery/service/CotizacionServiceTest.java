package com.uqdelivery.service;

import com.uqdelivery.domain.Tarifa;
import com.uqdelivery.dto.CotizacionRequest;
import com.uqdelivery.dto.CotizacionResponse;
import com.uqdelivery.exception.TarifaNoDisponibleException;
import com.uqdelivery.repository.TarifaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

/** Pruebas unitarias de HU-05 (reglas tarifarias) con Mockito, sin levantar Spring. */
class CotizacionServiceTest {

    private TarifaRepository repo;
    private DistanciaService distancia;
    private CotizacionService service;

    @BeforeEach
    void setUp() {
        repo = mock(TarifaRepository.class);
        distancia = mock(DistanciaService.class);
        service = new CotizacionService(repo, distancia);
        when(repo.findFirstByActivaTrueOrderByIdAsc()).thenReturn(Optional.of(tarifa("1200.00", "800.00")));
        conDistancia("10.00");
    }

    private static Tarifa tarifa(String porKm, String porKg) {
        return new Tarifa("Prueba", new BigDecimal("5000.00"), new BigDecimal(porKm), new BigDecimal(porKg),
                new BigDecimal("3000.00"), new BigDecimal("5000.00"), new BigDecimal("1.00"), true);
    }

    private void conDistancia(String km) {
        when(distancia.calcularKm(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(new BigDecimal(km));
    }

    private static CotizacionRequest req(String peso, String largo, String ancho, String alto,
                                         Boolean fragil, Boolean urgente) {
        return new CotizacionRequest(new BigDecimal(peso), new BigDecimal(largo), new BigDecimal(ancho),
                new BigDecimal(alto), 4.5339, -75.6811, 4.5500, -75.6600, fragil, urgente);
    }

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    //Peso facturable

    @Test
    void usaElPesoVolumetricoCuandoEsMayorQueElReal() {
        // 40 x 30 x 20 / 5000 = 4.80 kg > 1 kg
        CotizacionResponse c = service.cotizar(req("1", "40", "30", "20", null, null));
        assertEquals(bd("1.00"), c.pesoRealKg());
        assertEquals(bd("4.80"), c.pesoVolumetricoKg());
        assertEquals(bd("4.80"), c.pesoFacturableKg());
        assertEquals(bd("3840.00"), c.costoPeso());
        assertEquals(bd("12000.00"), c.costoDistancia());
        assertEquals(bd("20840.00"), c.total()); // 5000 + 12000 + 3840
    }

    @Test
    void usaElPesoRealCuandoEsMayorQueElVolumetrico() {
        // 10 x 10 x 10 / 5000 = 0.20 kg < 5 kg
        CotizacionResponse c = service.cotizar(req("5", "10", "10", "10", false, false));
        assertEquals(bd("0.20"), c.pesoVolumetricoKg());
        assertEquals(bd("5.00"), c.pesoFacturableKg());
        assertEquals(bd("4000.00"), c.costoPeso());
        assertEquals(bd("21000.00"), c.total()); // 5000 + 12000 + 4000
        assertTrue(c.recargos().isEmpty());
        assertEquals(bd("0.00"), c.totalRecargos());
    }

    //Recargos

    @Test
    void sumaElRecargoPorFragil() {
        CotizacionResponse c = service.cotizar(req("5", "10", "10", "10", true, false));
        assertEquals(List.of(new CotizacionResponse.Recargo("Fragil", bd("3000.00"))), c.recargos());
        assertEquals(bd("24000.00"), c.total());
    }

    @Test
    void sumaElRecargoPorUrgente() {
        CotizacionResponse c = service.cotizar(req("5", "10", "10", "10", false, true));
        assertEquals(List.of(new CotizacionResponse.Recargo("Urgente", bd("5000.00"))), c.recargos());
        assertEquals(bd("26000.00"), c.total());
    }

    @Test
    void sumaAmbosRecargosCuandoEsFragilYUrgente() {
        CotizacionResponse c = service.cotizar(req("5", "10", "10", "10", true, true));
        assertEquals(2, c.recargos().size());
        assertEquals(bd("8000.00"), c.totalRecargos());
        assertEquals(bd("29000.00"), c.total());
    }

    //Distancia

    @Test
    void conDistanciaCeroSoloCobraBaseYPeso() {
        conDistancia("0.00");
        CotizacionResponse c = service.cotizar(req("2", "10", "10", "10", null, null));
        assertEquals(bd("0.00"), c.distanciaKm());
        assertEquals(bd("0.00"), c.costoDistancia());
        assertEquals(bd("6600.00"), c.total()); // 5000 + 2 x 800
    }

    @Test
    void pasaLasCoordenadasDelRequestAlCalculoDeDistancia() {
        service.cotizar(req("1", "10", "10", "10", null, null));
        verify(distancia).calcularKm(4.5339, -75.6811, 4.5500, -75.6600);
    }

    //Redondeo

    @Test
    void redondeaCadaValorADosDecimalesConHalfUp() {
        when(repo.findFirstByActivaTrueOrderByIdAsc()).thenReturn(Optional.of(tarifa("1200.55", "799.99")));
        conDistancia("3.33");
        // volumetrico: 33.3 x 10 x 10 / 5000 = 0.666 -> 0.67; real 0.5
        CotizacionResponse c = service.cotizar(req("0.5", "33.3", "10", "10", null, null));
        assertEquals(bd("0.67"), c.pesoFacturableKg());
        assertEquals(bd("3997.83"), c.costoDistancia()); // 3.33 x 1200.55 = 3997.8315
        assertEquals(bd("535.99"), c.costoPeso());       // 0.67 x 799.99 = 535.9933
        assertEquals(bd("9533.82"), c.total());
        assertEquals(2, c.total().scale());
    }

    @Test
    void elPesoRealSeRedondeaHaciaArribaEnLaMitad() {
        CotizacionResponse c = service.cotizar(req("1.005", "1", "1", "1", null, null));
        assertEquals(bd("1.01"), c.pesoRealKg());
    }

    @Test
    void elPesoVolumetricoUsaElFactor5000() {
        assertEquals(bd("1.00"), CotizacionService.pesoVolumetrico(bd("50"), bd("10"), bd("10")));
    }

    //Respuesta

    @Test
    void laCotizacionEsUnValorEstimadoEnPesos() {
        CotizacionResponse c = service.cotizar(req("1", "10", "10", "10", null, null));
        assertTrue(c.estimado());
        assertEquals("COP", c.moneda());
        assertEquals("Prueba", c.tarifa());
        assertEquals(bd("5000.00"), c.tarifaBase());
        assertEquals(bd("1200.00"), c.costoPorKm());
        assertEquals(bd("800.00"), c.costoPorKg());
    }

    //Sin tarifa activa

    @Test
    void sinTarifaActivaLanzaExcepcionYNoCalculaDistancia() {
        when(repo.findFirstByActivaTrueOrderByIdAsc()).thenReturn(Optional.empty());
        TarifaNoDisponibleException ex = assertThrows(TarifaNoDisponibleException.class,
                () -> service.cotizar(req("1", "10", "10", "10", null, null)));
        assertTrue(ex.getMessage().contains("tarifa activa"));
        verifyNoInteractions(distancia);
    }
}
