package com.uqdelivery.service;

import com.uqdelivery.domain.Tarifa;
import com.uqdelivery.dto.TarifaRequest;
import com.uqdelivery.dto.TarifaResponse;
import com.uqdelivery.exception.BusinessRuleException;
import com.uqdelivery.exception.ResourceNotFoundException;
import com.uqdelivery.repository.TarifaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** Pruebas unitarias de HU-05: administracion de tarifas y regla de una sola tarifa activa. */
class TarifaServiceTest {

    private TarifaRepository repo;
    private TarifaService service;

    @BeforeEach
    void setUp() {
        repo = mock(TarifaRepository.class);
        service = new TarifaService(repo);
        when(repo.save(any(Tarifa.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Tarifa tarifa(long id, boolean activa) {
        Tarifa t = new Tarifa("Tarifa " + id, new BigDecimal("5000.00"), new BigDecimal("1200.00"),
                new BigDecimal("800.00"), new BigDecimal("3000.00"), new BigDecimal("5000.00"),
                new BigDecimal("1.00"), activa);
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    private static TarifaRequest req(Boolean activa) {
        return new TarifaRequest("  Tarifa nueva  ", new BigDecimal("6000"), new BigDecimal("1500"),
                new BigDecimal("900"), new BigDecimal("3500"), new BigDecimal("5500"), new BigDecimal("2"), activa);
    }

    @Test
    void listarDevuelveLasTarifasComoDto() {
        when(repo.findAllByOrderByIdAsc()).thenReturn(List.of(tarifa(1, true), tarifa(2, false)));
        List<TarifaResponse> lista = service.listar();
        assertEquals(2, lista.size());
        assertTrue(lista.get(0).activa());
        assertFalse(lista.get(1).activa());
    }

    @Test
    void actualizarGuardaLosValoresYMantieneUnaSolaActiva() {
        when(repo.findById(1L)).thenReturn(Optional.of(tarifa(1, true)));
        TarifaResponse r = service.actualizar(1L, req(null));
        assertEquals("Tarifa nueva", r.nombre());
        assertEquals(new BigDecimal("6000"), r.tarifaBase());
        assertEquals(new BigDecimal("1500"), r.costoPorKm());
        assertEquals(new BigDecimal("900"), r.costoPorKg());
        assertEquals(new BigDecimal("3500"), r.recargoFragil());
        assertEquals(new BigDecimal("5500"), r.recargoUrgente());
        assertEquals(new BigDecimal("2"), r.porcentajeSeguro());
        assertTrue(r.activa());
        verify(repo).desactivarOtras(1L);
    }

    @Test
    void activarUnaTarifaDesactivaLasDemas() {
        when(repo.findById(2L)).thenReturn(Optional.of(tarifa(2, false)));
        assertTrue(service.actualizar(2L, req(true)).activa());
        verify(repo).desactivarOtras(2L);
    }

    @Test
    void editarUnaTarifaInactivaNoTocaLaActiva() {
        when(repo.findById(2L)).thenReturn(Optional.of(tarifa(2, false)));
        assertFalse(service.actualizar(2L, req(false)).activa());
        verify(repo, never()).desactivarOtras(anyLong());
    }

    @Test
    void noSePuedeDesactivarLaTarifaActiva() {
        when(repo.findById(1L)).thenReturn(Optional.of(tarifa(1, true)));
        assertThrows(BusinessRuleException.class, () -> service.actualizar(1L, req(false)));
        verify(repo, never()).save(any());
    }

    @Test
    void tarifaInexistenteDa404() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.actualizar(99L, req(null)));
    }
}
