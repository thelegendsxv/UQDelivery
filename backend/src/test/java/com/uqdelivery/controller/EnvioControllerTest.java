package com.uqdelivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uqdelivery.domain.Role;
import com.uqdelivery.domain.User;
import com.uqdelivery.repository.EnvioRepository;
import com.uqdelivery.repository.TarifaRepository;
import com.uqdelivery.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion de HU-04 con MockMvc. Usa la tarifa que crea TarifaSeeder
 * (base 5000, 1200/km, 800/kg, fragil 3000, urgente 5000); origen = destino para que la distancia sea 0.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EnvioControllerTest {

    private static final String URL = "/api/envios";
    private static final String PASSWORD = "Clave1234";
    // 30 x 20 x 10 / 5000 = 1.20 kg volumetrico < 2 kg real -> 2 x 800 = 1600; distancia 0 (mismo punto)
    private static final String VALIDO = """
            {"pesoKg":2,"largoCm":30,"anchoCm":20,"altoCm":10,
             "origenDireccion":"Calle 1 # 2-3","origenLat":4.5339,"origenLng":-75.6811,
             "destinoDireccion":"Calle 1 # 2-3","destinoLat":4.5339,"destinoLng":-75.6811,
             "receptorNombre":"Juan Perez","receptorTelefono":"3001234567","receptorEmail":"juan@mail.com",
             "descripcion":"Documentos","esFragil":false,"esUrgente":false}""";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired EnvioRepository envios;
    @Autowired TarifaRepository tarifas;
    @Autowired PasswordEncoder encoder;

    private String token(String email, Role role) throws Exception {
        users.save(new User("Usuario Prueba", email, encoder.encode(PASSWORD), role));
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(body).get("token").asText();
    }

    //Criterio 1: con datos completos, crea el envio con el precio estimado

    @Test
    void clienteCreaUnEnvioYRecibeElCodigoYElPrecio() throws Exception {
        String token = token("hu04.cliente@mail.com", Role.CLIENTE_EMISOR);

        mvc.perform(post(URL).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoSeguimiento", startsWith("UQD-")))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_ASIGNACION"))
                .andExpect(jsonPath("$.distanciaKm").value(0.0))
                .andExpect(jsonPath("$.precioCotizado").value(6600.0)) // 5000 + 0 + 2 x 800
                .andExpect(jsonPath("$.receptorNombre").value("Juan Perez"));
    }

    //Criterio 2: si falta un dato, 400 y no se crea ningun registro

    @Test
    void sinDatosDevuelve400YNoCreaNingunRegistro() throws Exception {
        String token = token("hu04.incompleto@mail.com", Role.CLIENTE_EMISOR);
        long antes = envios.count();

        mvc.perform(post(URL).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.largoCm").exists())
                .andExpect(jsonPath("$.fieldErrors.origenDireccion").exists())
                .andExpect(jsonPath("$.fieldErrors.receptorNombre").exists())
                .andExpect(jsonPath("$.fieldErrors.receptorTelefono").exists())
                .andExpect(jsonPath("$.codigoSeguimiento").doesNotExist());

        assertEqualsCount(antes);
    }

    @Test
    void elNombreDelReceptorRechazaHtml() throws Exception {
        String token = token("hu04.xss@mail.com", Role.CLIENTE_EMISOR);
        mvc.perform(post(URL).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content(VALIDO.replace("Juan Perez", "<script>alert(1)</script>")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.receptorNombre").exists());
    }

    //Sin tarifa activa

    @Test
    void sinTarifaActivaDevuelve503YNoCreaElEnvio() throws Exception {
        String token = token("hu04.sintarifa@mail.com", Role.CLIENTE_EMISOR);
        long antes = envios.count();
        tarifas.findAll().forEach(t -> t.setActiva(false));
        tarifas.flush();

        mvc.perform(post(URL).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(containsString("tarifa activa")));

        assertEqualsCount(antes);
    }

    //Permisos

    @Test
    void repartidorRecibe403() throws Exception {
        String token = token("hu04.rep@mail.com", Role.REPARTIDOR);
        mvc.perform(post(URL).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorRecibe403() throws Exception {
        String token = token("hu04.admin@mail.com", Role.ADMINISTRADOR);
        mvc.perform(post(URL).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void otroRolConCuerpoInvalidoRecibe403YNo400() throws Exception {
        String token = token("hu04.admin2@mail.com", Role.ADMINISTRADOR);
        mvc.perform(post(URL).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void sinTokenRecibe401() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALIDO)).andExpect(status().isUnauthorized());
        mvc.perform(get(URL + "/mis-envios")).andExpect(status().isUnauthorized());
    }

    //Consultas

    @Test
    void elClienteSoloVeSusPropiosEnviosEnMisEnvios() throws Exception {
        String tokenA = token("hu04.a@mail.com", Role.CLIENTE_EMISOR);
        String tokenB = token("hu04.b@mail.com", Role.CLIENTE_EMISOR);

        mvc.perform(post(URL).header("Authorization", tokenA).contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated());

        mvc.perform(get(URL + "/mis-envios").header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get(URL + "/mis-envios").header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void consultarPorCodigoDeOtroClienteDevuelve404() throws Exception {
        String tokenA = token("hu04.dueno@mail.com", Role.CLIENTE_EMISOR);
        String tokenB = token("hu04.intruso@mail.com", Role.CLIENTE_EMISOR);

        String body = mvc.perform(post(URL).header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andReturn().getResponse().getContentAsString();
        String codigo = mapper.readTree(body).get("codigoSeguimiento").asText();

        mvc.perform(get(URL + "/" + codigo).header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoSeguimiento").value(codigo));

        mvc.perform(get(URL + "/" + codigo).header("Authorization", tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultarUnCodigoInexistenteDevuelve404() throws Exception {
        String token = token("hu04.consulta@mail.com", Role.CLIENTE_EMISOR);
        mvc.perform(get(URL + "/UQD-NOEXISTE").header("Authorization", token))
                .andExpect(status().isNotFound());
    }

    @Test
    void laDocumentacionOpenApiIncluyeLosEndpointsDeHu04() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/envios")))
                .andExpect(content().string(containsString("/api/envios/mis-envios")));
    }

    private void assertEqualsCount(long esperado) {
        if (envios.count() != esperado) {
            throw new AssertionError("Se creo un registro de envio cuando no debia: esperaba " + esperado
                    + " pero hay " + envios.count());
        }
    }
}
