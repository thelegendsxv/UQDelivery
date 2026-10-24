package com.uqdelivery.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired MockMvc mvc;

    private static String registerJson(String nombre, String email, String password) {
        return String.format("{\"nombre\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}", nombre, email, password);
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
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("Ana Perez", "it4@mail.com", "Clave1234"))).andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Otra Persona", "IT4@mail.com", "Clave1234")))
                .andExpect(status().isConflict());
    }
}

