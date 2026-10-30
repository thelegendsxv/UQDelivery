package com.uqdelivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uqdelivery.domain.Role;
import com.uqdelivery.domain.User;
import com.uqdelivery.repository.TarifaRepository;
import com.uqdelivery.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion de HU-05 con MockMvc. Usa la tarifa que crea TarifaSeeder
 * (base 5000, 1200/km, 800/kg, fragil 3000, urgente 5000). Origen = destino para que la distancia sea 0.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CotizacionControllerTest {

    private static final String URL = "/api/cotizaciones";
    // 30 x 20 x 10 / 5000 = 1.20 kg volumetrico < 2 kg real -> 2 x 800 = 1600
    private static final String VALIDA = """
            {"pesoKg":2,"largoCm":30,"anchoCm":20,"altoCm":10,
             "origenLat":4.5339,"origenLng":-75.6811,"destinoLat":4.5339,"destinoLng":-75.6811,
             "esFragil":true,"esUrgente":true}""";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired TarifaRepository tarifas;
    @Autowired PasswordEncoder encoder;

    private String token(String email, Role role) throws Exception {
        users.save(new User("Usuario Prueba", email, encoder.encode("Clave1234"), role));
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Clave1234\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(body).get("token").asText();
    }

    //Criterio 1: con datos completos devuelve el precio estimado

    @Test
    @WithMockUser(roles = "CLIENTE_EMISOR")
    void clienteObtieneElDesgloseDelPrecioEstimado() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimado").value(true))
                .andExpect(jsonPath("$.moneda").value("COP"))
                .andExpect(jsonPath("$.tarifaBase").value(5000.0))
                .andExpect(jsonPath("$.distanciaKm").value(0.0))
                .andExpect(jsonPath("$.pesoVolumetricoKg").value(1.2))
                .andExpect(jsonPath("$.pesoFacturableKg").value(2.0))
                .andExpect(jsonPath("$.costoPeso").value(1600.0))
                .andExpect(jsonPath("$.recargos.length()").value(2))
                .andExpect(jsonPath("$.totalRecargos").value(8000.0))
                .andExpect(jsonPath("$.total").value(14600.0));
    }

    @Test
    void clienteConTokenRealPuedeCotizarSinCasillas() throws Exception {
        mvc.perform(post(URL).header("Authorization", token("hu05.cliente@mail.com", Role.CLIENTE_EMISOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALIDA.replace("true", "false")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recargos.length()").value(0))
                .andExpect(jsonPath("$.total").value(6600.0));
    }

    //Criterio 2: si falta un dato, 400 con los datos pendientes y sin precio

    @Test
    @WithMockUser(roles = "CLIENTE_EMISOR")
    void sinDatosDevuelve400ConLosCamposPendientesYSinPrecio() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"pesoKg\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.largoCm").value("El largo es obligatorio"))
                .andExpect(jsonPath("$.fieldErrors.anchoCm").exists())
                .andExpect(jsonPath("$.fieldErrors.altoCm").exists())
                .andExpect(jsonPath("$.fieldErrors.origenLat").exists())
                .andExpect(jsonPath("$.fieldErrors.origenLng").exists())
                .andExpect(jsonPath("$.fieldErrors.destinoLat").exists())
                .andExpect(jsonPath("$.fieldErrors.destinoLng").exists())
                .andExpect(jsonPath("$.fieldErrors.pesoKg").doesNotExist())
                .andExpect(jsonPath("$.total").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "CLIENTE_EMISOR")
    void valoresFueraDeRangoDevuelven400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(VALIDA.replace("\"pesoKg\":2", "\"pesoKg\":-1").replace("4.5339,\"origenLng\"", "95,\"origenLng\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.pesoKg").value("El peso debe ser mayor que 0"))
                .andExpect(jsonPath("$.fieldErrors.origenLat").exists())
                .andExpect(jsonPath("$.total").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "CLIENTE_EMISOR")
    void cuerpoMalFormadoDevuelve400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"pesoKg\":\"dos\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.total").doesNotExist());
    }

    //Sin tarifa activa

    @Test
    @WithMockUser(roles = "CLIENTE_EMISOR")
    void sinTarifaActivaDevuelve503() throws Exception {
        tarifas.findAll().forEach(t -> t.setActiva(false));
        tarifas.flush();
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(containsString("tarifa activa")))
                .andExpect(jsonPath("$.total").doesNotExist());
    }

    //Permisos

    @Test
    @WithMockUser(roles = "REPARTIDOR")
    void repartidorRecibe403() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorRecibe403() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isForbidden());
    }

    @Test
    void otroRolConCuerpoInvalidoRecibe403YNo400() throws Exception {
        mvc.perform(post(URL).header("Authorization", token("hu05.admin@mail.com", Role.ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void sinTokenRecibe401() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laDocumentacionOpenApiIncluyeLosEndpointsDeHu05() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/cotizaciones")))
                .andExpect(content().string(containsString("/api/tarifas/{id}")));
    }
}
