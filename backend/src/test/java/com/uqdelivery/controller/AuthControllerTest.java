package com.uqdelivery.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private static String registerJson(String nombre, String email, String password) {
        return String.format("{\"nombre\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}", nombre, email, password);
    }

    private static String loginJson(String email, String password) {
        return String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
    }

    private void register(String email) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Ana Perez", email, "Clave1234"))).andExpect(status().isCreated());
    }

    @Test
    void registerWithValidDataReturns201WithoutPassword() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Ana Perez", "it1@mail.com", "Clave1234")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("it1@mail.com"))
                .andExpect(jsonPath("$.role").value("CLIENTE_EMISOR"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registerIgnoresRoleSentByClient() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"email\":\"it2@mail.com\",\"password\":\"Clave1234\",\"role\":\"ADMINISTRADOR\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CLIENTE_EMISOR"));
    }

    @Test
    void registerWithInvalidEmailAndWeakPasswordReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Ana Perez", "no-es-correo", "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void registerRejectsHtmlInName() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("<script>alert(1)</script>", "it3@mail.com", "Clave1234")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nombre").exists());
    }

    @Test
    void registerWithDuplicateEmailReturns409() throws Exception {
        register("it4@mail.com");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Otra Persona", "IT4@mail.com", "Clave1234")))
                .andExpect(status().isConflict());
    }

    @Test
    void sqlInjectionInLoginIsJustAWrongCredential() throws Exception {
        register("it5@mail.com");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("it5@mail.com' OR '1'='1", "' OR '1'='1")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithCorrectCredentialsReturnsTokenAndMeWorks() throws Exception {
        register("it6@mail.com");
        MvcResult res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("it6@mail.com", "Clave1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.role").value("CLIENTE_EMISOR"))
                .andReturn();
        JsonNode body = mapper.readTree(res.getResponse().getContentAsString());

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + body.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("it6@mail.com"));
    }

    @Test
    void loginWithWrongPasswordReturns401WithGenericMessage() throws Exception {
        register("it7@mail.com");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("it7@mail.com", "Incorrecta1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contrasena incorrectos"));
    }

    @Test
    void loginWithBlankFieldsReturns400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithGarbageTokenReturns401() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer basura.token.falso"))
                .andExpect(status().isUnauthorized());
    }
}
