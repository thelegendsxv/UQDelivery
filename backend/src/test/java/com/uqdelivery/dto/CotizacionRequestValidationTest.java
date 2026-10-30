package com.uqdelivery.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas unitarias de HU-05, criterio 2: reglas de los datos requeridos para cotizar. */
class CotizacionRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    private static Set<String> camposConError(Object dto) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getPropertyPath).map(Object::toString).collect(Collectors.toSet());
    }

    private static CotizacionRequest valido() {
        return new CotizacionRequest(new BigDecimal("2"), new BigDecimal("30"), new BigDecimal("20"),
                new BigDecimal("10"), 4.5339, -75.6811, 4.55, -75.66, null, null);
    }

    @Test
    void unaSolicitudCompletaEsValidaYLasCasillasSonOpcionales() {
        CotizacionRequest r = valido();
        assertTrue(camposConError(r).isEmpty());
        assertFalse(r.fragil());
        assertFalse(r.urgente());
    }

    @Test
    void sinDatosReportaTodosLosCamposPendientes() {
        CotizacionRequest vacio = new CotizacionRequest(null, null, null, null, null, null, null, null, null, null);
        assertEquals(Set.of("pesoKg", "largoCm", "anchoCm", "altoCm",
                "origenLat", "origenLng", "destinoLat", "destinoLng"), camposConError(vacio));
    }

    @Test
    void rechazaPesoYMedidasEnCeroONegativos() {
        CotizacionRequest r = new CotizacionRequest(BigDecimal.ZERO, new BigDecimal("-1"), new BigDecimal("20"),
                new BigDecimal("10"), 4.5, -75.6, 4.5, -75.6, true, true);
        assertEquals(Set.of("pesoKg", "largoCm"), camposConError(r));
    }

    @Test
    void rechazaCoordenadasFueraDeRango() {
        CotizacionRequest r = new CotizacionRequest(new BigDecimal("2"), new BigDecimal("30"), new BigDecimal("20"),
                new BigDecimal("10"), 90.5, -180.1, -91.0, 181.0, false, false);
        assertEquals(Set.of("origenLat", "origenLng", "destinoLat", "destinoLng"), camposConError(r));
    }

    @Test
    void aceptaLosLimitesExactosDeLasCoordenadas() {
        CotizacionRequest r = new CotizacionRequest(new BigDecimal("2"), new BigDecimal("30"), new BigDecimal("20"),
                new BigDecimal("10"), 90.0, -180.0, -90.0, 180.0, false, false);
        assertTrue(camposConError(r).isEmpty());
    }
}
