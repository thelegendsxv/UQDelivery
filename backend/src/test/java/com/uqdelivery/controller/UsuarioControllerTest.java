package com.uqdelivery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uqdelivery.domain.Role;
import com.uqdelivery.domain.User;
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

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion de HU-02 con MockMvc.
 * Usa el administrador que crea AdminSeeder (admin@uqdelivery.com / Admin12345, valores de desarrollo).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioControllerTest {

    private static final String ADMIN_EMAIL = "admin@uqdelivery.com";
    private static final String ADMIN_PASSWORD = "Admin12345";
    private static final String PASSWORD = "Clave1234";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    //utilidades

    private User crearUsuario(String email, Role role) {
        return users.save(new User("Usuario Prueba", email, encoder.encode(PASSWORD), role));
    }

    private String token(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(body).get("token").asText();
    }

    private String adminToken() throws Exception {
        return token(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    //Criterio 1: el admin asigna o modifica el rol

    @Test
    void adminCambiaRolYSeReflejaEnElPerfilDelUsuario() throws Exception {
        User u = crearUsuario("hu02.rol@mail.com", Role.CLIENTE_EMISOR);
        String tokenUsuario = token("hu02.rol@mail.com", PASSWORD);

        mvc.perform(put("/api/usuarios/" + u.getId() + "/rol").header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"REPARTIDOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("REPARTIDOR"));

        // El perfil del usuario refleja el cambio, incluso con su token emitido antes del cambio.
        mvc.perform(get("/api/usuarios/me").header("Authorization", tokenUsuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("REPARTIDOR"));
    }

    @Test
    void elNuevoRolAplicaDeInmediatoSobreUnTokenAnterior() throws Exception {
        User u = crearUsuario("hu02.promo@mail.com", Role.CLIENTE_EMISOR);
        String tokenUsuario = token("hu02.promo@mail.com", PASSWORD);

        mvc.perform(get("/api/usuarios").header("Authorization", tokenUsuario)).andExpect(status().isForbidden());

        mvc.perform(put("/api/usuarios/" + u.getId() + "/rol").header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/usuarios").header("Authorization", tokenUsuario)).andExpect(status().isOk());
    }

    @Test
    void adminNoPuedeCambiarSuPropioRol() throws Exception {
        Long adminId = users.findByEmail(ADMIN_EMAIL).orElseThrow().getId();
        mvc.perform(put("/api/usuarios/" + adminId + "/rol").header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"CLIENTE_EMISOR\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No puedes cambiar tu propio rol"));
    }

    @Test
    void rolInvalidoOAusenteDevuelve400() throws Exception {
        User u = crearUsuario("hu02.inv@mail.com", Role.CLIENTE_EMISOR);
        mvc.perform(put("/api/usuarios/" + u.getId() + "/rol").header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"SUPERUSUARIO\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/usuarios/" + u.getId() + "/rol").header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.role").exists());
    }

    @Test
    void usuarioInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/usuarios/999999").header("Authorization", adminToken()))
                .andExpect(status().isNotFound());
    }

    //Criterio 2: acceso denegado por rol

    @Test
    void clienteEmisorRecibe403AlListarUsuarios() throws Exception {
        crearUsuario("hu02.cli@mail.com", Role.CLIENTE_EMISOR);
        mvc.perform(get("/api/usuarios").header("Authorization", token("hu02.cli@mail.com", PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("No tienes permiso para realizar esta accion"));
    }

    @Test
    @WithMockUser(username = "rep@mail.com", roles = "REPARTIDOR")
    void repartidorRecibe403AlCambiarRoles() throws Exception {
        mvc.perform(put("/api/usuarios/1/rol").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cli@mail.com", roles = "CLIENTE_EMISOR")
    void clienteRecibe403AlCambiarEstadoOCrearUsuarios() throws Exception {
        mvc.perform(patch("/api/usuarios/1/estado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"activo\":false}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"email\":\"x@mail.com\",\"password\":\"Clave1234\",\"role\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/me")).andExpect(status().isUnauthorized());
    }

    //Listado y filtros

    @Test
    void adminListaUsuariosFiltrandoPorRol() throws Exception {
        crearUsuario("hu02.rep1@mail.com", Role.REPARTIDOR);
        crearUsuario("hu02.cli2@mail.com", Role.CLIENTE_EMISOR);
        mvc.perform(get("/api/usuarios").param("rol", "REPARTIDOR").header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].role", everyItem(is("REPARTIDOR"))))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    @Test
    void adminBuscaPorTexto() throws Exception {
        crearUsuario("hu02.buscable@mail.com", Role.CLIENTE_EMISOR);
        mvc.perform(get("/api/usuarios").param("q", "BUSCABLE").header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("hu02.buscable@mail.com"));
    }

    @Test
    void filtroConRolInexistenteDevuelve400() throws Exception {
        mvc.perform(get("/api/usuarios").param("rol", "SUPER").header("Authorization", adminToken()))
                .andExpect(status().isBadRequest());
    }

    // Activar / desactivar

    @Test
    void usuarioDesactivadoPierdeElAccesoDeInmediato() throws Exception {
        User u = crearUsuario("hu02.off@mail.com", Role.CLIENTE_EMISOR);
        String tokenUsuario = token("hu02.off@mail.com", PASSWORD);

        mvc.perform(patch("/api/usuarios/" + u.getId() + "/estado").header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mvc.perform(get("/api/usuarios/me").header("Authorization", tokenUsuario))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"hu02.off@mail.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    //Crear usuario con rol

    @Test
    void adminCreaRepartidor() throws Exception {
        mvc.perform(post("/api/usuarios").header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Pedro Reparto\",\"email\":\"hu02.newrep@mail.com\","
                                + "\"password\":\"Clave1234\",\"role\":\"REPARTIDOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("REPARTIDOR"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void crearUsuarioConDatosInvalidosDevuelve400() throws Exception {
        mvc.perform(post("/api/usuarios").header("Authorization", adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"<b>x</b>\",\"email\":\"malo\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nombre").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.role").exists());
    }

    // Perfil propio

    @Test
    void usuarioActualizaSuPerfilSinPoderCambiarCorreo() throws Exception {
        crearUsuario("hu02.perfil@mail.com", Role.CLIENTE_EMISOR);
        String t = token("hu02.perfil@mail.com", PASSWORD);

        mvc.perform(put("/api/usuarios/me").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Gomez\",\"telefono\":\"+57 300 123 4567\","
                                + "\"direccion\":\"Calle 10 # 5-20\",\"email\":\"otro@mail.com\",\"role\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana Gomez"))
                .andExpect(jsonPath("$.telefono").value("+57 300 123 4567"))
                .andExpect(jsonPath("$.email").value("hu02.perfil@mail.com"))
                .andExpect(jsonPath("$.role").value("CLIENTE_EMISOR"));
    }

    @Test
    void perfilRechazaHtmlYTelefonoInvalido() throws Exception {
        crearUsuario("hu02.xss@mail.com", Role.CLIENTE_EMISOR);
        mvc.perform(put("/api/usuarios/me").header("Authorization", token("hu02.xss@mail.com", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"<script>alert(1)</script>\",\"telefono\":\"abc\","
                                + "\"direccion\":\"<img src=x onerror=alert(1)>\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nombre").exists())
                .andExpect(jsonPath("$.fieldErrors.telefono").exists())
                .andExpect(jsonPath("$.fieldErrors.direccion").exists());
    }
}