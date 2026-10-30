package com.uqdelivery.controller;

import com.uqdelivery.service.ReseteoContrasenaEmailService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-03 · Pruebas de integracion (MockMvc) del flujo completo de recuperacion. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReseteoContrasenaControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ReseteoContrasenaEmailService email; // captura el enlace en lugar de enviar un correo

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void registrar(String correo) throws Exception {
        postJson("/api/auth/register",
                "{\"nombre\":\"Ana Perez\",\"email\":\"" + correo + "\",\"password\":\"Clave1234\"}")
                .andExpect(status().isCreated());
    }

    private ResultActions login(String correo, String password) throws Exception {
        return postJson("/api/auth/login", "{\"email\":\"" + correo + "\",\"password\":\"" + password + "\"}");
    }

    private ResultActions forgot(String correo) throws Exception {
        return postJson("/api/auth/forgot-password", "{\"email\":\"" + correo + "\"}");
    }

    private ResultActions reset(String token, String password) throws Exception {
        return postJson("/api/auth/reset-password", "{\"token\":\"" + token + "\",\"password\":\"" + password + "\"}");
    }

    /** Pide la recuperacion y devuelve el token del ultimo enlace "enviado". */
    private String tokenEnviadoA(String correo) throws Exception {
        forgot(correo).andExpect(status().isOk());
        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(email, atLeastOnce()).enviarEnlace(eq(correo), anyString(), enlace.capture(), anyLong());
        String url = enlace.getValue();
        return url.substring(url.indexOf("token=") + 6);
    }

    @Test
    void flujoCompletoPermiteIniciarSesionConLaNuevaContrasena() throws Exception {
        registrar("hu3a@mail.com");
        String token = tokenEnviadoA("hu3a@mail.com");

        reset(token, "NuevaClave99").andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(ReseteoContrasenaController.MENSAJE_RESTABLECIDA));

        login("hu3a@mail.com", "NuevaClave99").andExpect(status().isOk()).andExpect(jsonPath("$.token").exists());
        login("hu3a@mail.com", "Clave1234").andExpect(status().isUnauthorized());
    }

    @Test
    void correoInexistenteRecibeLaMismaRespuestaYNoSeEnviaNada() throws Exception {
        registrar("hu3b@mail.com");

        forgot("hu3b@mail.com").andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(ReseteoContrasenaController.MENSAJE_SOLICITUD));
        forgot("no-existe-hu3@mail.com").andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(ReseteoContrasenaController.MENSAJE_SOLICITUD));

        verify(email, never()).enviarEnlace(eq("no-existe-hu3@mail.com"), anyString(), anyString(), anyLong());
    }

    @Test
    void tokenYaUsadoDevuelve400() throws Exception {
        registrar("hu3c@mail.com");
        String token = tokenEnviadoA("hu3c@mail.com");

        reset(token, "NuevaClave99").andExpect(status().isOk());
        reset(token, "OtraClave77").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("no es valido")));
    }

    @Test
    void pedirUnEnlaceNuevoInvalidaElAnterior() throws Exception {
        registrar("hu3d@mail.com");
        String primero = tokenEnviadoA("hu3d@mail.com");
        String segundo = tokenEnviadoA("hu3d@mail.com");

        reset(primero, "NuevaClave99").andExpect(status().isBadRequest());
        reset(segundo, "NuevaClave99").andExpect(status().isOk());
    }

    @Test
    void tokenInventadoDevuelve400() throws Exception {
        reset("A".repeat(43), "NuevaClave99").andExpect(status().isBadRequest());
    }

    @Test
    void contrasenaDebilDevuelve400ConErrorDeCampo() throws Exception {
        registrar("hu3e@mail.com");
        String token = tokenEnviadoA("hu3e@mail.com");

        reset(token, "debil").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
        reset(token, "NuevaClave99").andExpect(status().isOk()); // el token sigue sirviendo
    }

    @Test
    void datosMalFormadosDevuelven400() throws Exception {
        forgot("no-es-correo").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        reset("<script>", "NuevaClave99").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.token").exists());
    }

    @Test
    void demasiadasSolicitudesDevuelven429() throws Exception {
        for (int i = 0; i < 5; i++) forgot("spam-hu3@mail.com").andExpect(status().isOk());
        forgot("spam-hu3@mail.com").andExpect(status().isTooManyRequests());
    }

    @Test
    void endpointsSonPublicosSinToken() throws Exception {
        forgot("publico-hu3@mail.com").andExpect(status().isOk());
    }
}
