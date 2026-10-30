package com.uqdelivery.controller;

import com.uqdelivery.domain.Tarifa;
import com.uqdelivery.repository.TarifaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de integracion de HU-05: /api/tarifas solo para ADMINISTRADOR. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TarifaControllerTest {

    private static final String BODY = """
            {"nombre":"Tarifa ajustada","tarifaBase":6000,"costoPorKm":1300,"costoPorKg":850,
             "recargoFragil":3500,"recargoUrgente":5500,"porcentajeSeguro":1.5%s}""";

    @Autowired MockMvc mvc;
    @Autowired TarifaRepository tarifas;

    private Long idActiva() {
        return tarifas.findFirstByActivaTrueOrderByIdAsc().orElseThrow().getId();
    }

    private static String body(String extra) {
        return BODY.formatted(extra);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminListaLasTarifas() throws Exception {
        mvc.perform(get("/api/tarifas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activa").value(true))
                .andExpect(jsonPath("$[0].tarifaBase").exists());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void adminActualizaLaTarifaActiva() throws Exception {
        mvc.perform(put("/api/tarifas/" + idActiva()).contentType(MediaType.APPLICATION_JSON).content(body("")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Tarifa ajustada"))
                .andExpect(jsonPath("$.tarifaBase").value(6000))
                .andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void activarOtraTarifaDejaUnaSolaActiva() throws Exception {
        Long anterior = idActiva();
        Tarifa otra = tarifas.save(new Tarifa("Otra", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, false));
        mvc.perform(put("/api/tarifas/" + otra.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(",\"activa\":true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activa").value(true));
        assertEquals(otra.getId(), idActiva());
        assertEquals(false, tarifas.findById(anterior).orElseThrow().isActiva());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void desactivarLaUnicaTarifaActivaDevuelve409() throws Exception {
        mvc.perform(put("/api/tarifas/" + idActiva()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(",\"activa\":false")))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void valoresNegativosOFaltantesDevuelven400() throws Exception {
        mvc.perform(put("/api/tarifas/" + idActiva()).contentType(MediaType.APPLICATION_JSON)
                        .content(body("").replace("\"tarifaBase\":6000", "\"tarifaBase\":-1")
                                .replace("\"nombre\":\"Tarifa ajustada\",", "\"nombre\":\"<b>x</b>\",")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.tarifaBase").value("La tarifa base no puede ser negativa"))
                .andExpect(jsonPath("$.fieldErrors.nombre").exists());
        mvc.perform(put("/api/tarifas/" + idActiva()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.costoPorKm").exists());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void tarifaInexistenteDevuelve404() throws Exception {
        mvc.perform(put("/api/tarifas/999999").contentType(MediaType.APPLICATION_JSON).content(body("")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "CLIENTE_EMISOR")
    void clienteRecibe403() throws Exception {
        mvc.perform(get("/api/tarifas")).andExpect(status().isForbidden());
        mvc.perform(put("/api/tarifas/" + idActiva()).contentType(MediaType.APPLICATION_JSON).content(body("")))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "REPARTIDOR")
    void repartidorRecibe403() throws Exception {
        mvc.perform(get("/api/tarifas")).andExpect(status().isForbidden());
    }

    @Test
    void sinTokenRecibe401() throws Exception {
        mvc.perform(get("/api/tarifas")).andExpect(status().isUnauthorized());
    }
}
