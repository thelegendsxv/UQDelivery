package com.uqdelivery.controller;

import com.uqdelivery.dto.LugarResponse;
import com.uqdelivery.service.TomTomMapasClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Pruebas de /api/lugares con MockMvc; el proveedor de mapas se simula (sin red ni clave). */
@SpringBootTest
@AutoConfigureMockMvc
class LugarControllerTest {

    @Autowired MockMvc mvc;
    @MockBean TomTomMapasClient mapas;

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/api/lugares").param("q", "Granada")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CLIENTE_EMISOR")
    void devuelveLosLugaresEncontrados() throws Exception {
        when(mapas.disponible()).thenReturn(true);
        when(mapas.buscar("Granada")).thenReturn(List.of(
                new LugarResponse("1", "Granada", "Armenia", 4.5533, -75.6606)));
        mvc.perform(get("/api/lugares").param("q", "  Granada "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titulo").value("Granada"))
                .andExpect(jsonPath("$[0].subtitulo").value("Armenia"))
                .andExpect(jsonPath("$[0].lat").value(4.5533));
    }

    @Test
    @WithMockUser(roles = "CLIENTE_EMISOR")
    void conMenosDeTresLetrasDevuelveListaVaciaSinConsultar() throws Exception {
        mvc.perform(get("/api/lugares").param("q", "ab"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(mapas, never()).buscar(anyString());
    }

    @Test
    @WithMockUser(roles = "CLIENTE_EMISOR")
    void sinClaveConfiguradaResponde503() throws Exception {
        when(mapas.disponible()).thenReturn(false);
        mvc.perform(get("/api/lugares").param("q", "Granada"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    @WithMockUser(roles = "CLIENTE_EMISOR")
    void laUniversidadDelQuindioDevuelveSusEntradasSinConsultarAlProveedor() throws Exception {
        mvc.perform(get("/api/lugares").param("q", "Universidad del Quindío"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("uq-ingenieria"))
                .andExpect(jsonPath("$[0].subtitulo").value("Armenia"));
        verify(mapas, never()).buscar(anyString());
    }
}
