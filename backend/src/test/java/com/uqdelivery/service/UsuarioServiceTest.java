package com.uqdelivery.service;

import com.uqdelivery.domain.Role;
import com.uqdelivery.domain.User;
import com.uqdelivery.dto.ActualizarPerfilRequest;
import com.uqdelivery.dto.CrearUsuarioRequest;
import com.uqdelivery.dto.PageResponse;
import com.uqdelivery.dto.UsuarioResponse;
import com.uqdelivery.exception.BusinessRuleException;
import com.uqdelivery.exception.DuplicateEmailException;
import com.uqdelivery.exception.ResourceNotFoundException;
import com.uqdelivery.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Pruebas unitarias de HU-02 (reglas de negocio) con Mockito, sin levantar Spring. */
class UsuarioServiceTest {

    private static final String ADMIN_EMAIL = "admin@mail.com";

    private UserRepository repo;
    private PasswordEncoder encoder;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        repo = mock(UserRepository.class);
        encoder = new BCryptPasswordEncoder(4);
        service = new UsuarioService(repo, encoder);
        when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static User user(long id, String email, Role role) {
        User u = new User("Usuario " + id, email, "hash", role);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    //Criterio 1: asignar / modificar rol

    @Test
    void cambiarRolGuardaElNuevoRol() {
        User cliente = user(2, "cliente@mail.com", Role.CLIENTE_EMISOR);
        when(repo.findById(2L)).thenReturn(Optional.of(cliente));

        UsuarioResponse res = service.cambiarRol(2L, Role.REPARTIDOR, ADMIN_EMAIL);

        assertEquals(Role.REPARTIDOR, res.role());
        verify(repo).save(cliente);
    }

    @Test
    void cambiarRolAlMismoRolNoGuarda() {
        when(repo.findById(2L)).thenReturn(Optional.of(user(2, "c@mail.com", Role.REPARTIDOR)));

        assertEquals(Role.REPARTIDOR, service.cambiarRol(2L, Role.REPARTIDOR, ADMIN_EMAIL).role());
        verify(repo, never()).save(any());
    }

    @Test
    void adminNoPuedeCambiarSuPropioRol() {
        when(repo.findById(1L)).thenReturn(Optional.of(user(1, ADMIN_EMAIL, Role.ADMINISTRADOR)));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.cambiarRol(1L, Role.CLIENTE_EMISOR, "ADMIN@mail.com"));
        assertEquals("No puedes cambiar tu propio rol", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void noSePuedeQuitarElRolAlUltimoAdministradorActivo() {
        when(repo.findById(5L)).thenReturn(Optional.of(user(5, "otro.admin@mail.com", Role.ADMINISTRADOR)));
        when(repo.countByRoleAndActivoTrue(Role.ADMINISTRADOR)).thenReturn(1L);

        assertThrows(BusinessRuleException.class, () -> service.cambiarRol(5L, Role.REPARTIDOR, ADMIN_EMAIL));
    }

    @Test
    void seLePuedeQuitarElRolAUnAdminSiQuedanOtros() {
        when(repo.findById(5L)).thenReturn(Optional.of(user(5, "otro.admin@mail.com", Role.ADMINISTRADOR)));
        when(repo.countByRoleAndActivoTrue(Role.ADMINISTRADOR)).thenReturn(2L);

        assertEquals(Role.CLIENTE_EMISOR, service.cambiarRol(5L, Role.CLIENTE_EMISOR, ADMIN_EMAIL).role());
    }

    @Test
    void cambiarRolDeUsuarioInexistenteLanza404() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.cambiarRol(99L, Role.REPARTIDOR, ADMIN_EMAIL));
    }

    // Activar / desactivar

    @Test
    void desactivarUsuario() {
        User u = user(3, "r@mail.com", Role.REPARTIDOR);
        when(repo.findById(3L)).thenReturn(Optional.of(u));

        assertFalse(service.cambiarEstado(3L, false, ADMIN_EMAIL).activo());
        assertFalse(u.isActivo());
    }

    @Test
    void reactivarUsuario() {
        User u = user(3, "r@mail.com", Role.REPARTIDOR);
        u.setActivo(false);
        when(repo.findById(3L)).thenReturn(Optional.of(u));

        assertTrue(service.cambiarEstado(3L, true, ADMIN_EMAIL).activo());
    }

    @Test
    void cambiarAlMismoEstadoNoGuarda() {
        when(repo.findById(3L)).thenReturn(Optional.of(user(3, "r@mail.com", Role.REPARTIDOR)));
        assertTrue(service.cambiarEstado(3L, true, ADMIN_EMAIL).activo());
        verify(repo, never()).save(any());
    }

    @Test
    void adminNoPuedeDesactivarse() {
        when(repo.findById(1L)).thenReturn(Optional.of(user(1, ADMIN_EMAIL, Role.ADMINISTRADOR)));
        assertThrows(BusinessRuleException.class, () -> service.cambiarEstado(1L, false, ADMIN_EMAIL));
    }

    @Test
    void noSePuedeDesactivarAlUltimoAdministrador() {
        when(repo.findById(5L)).thenReturn(Optional.of(user(5, "otro.admin@mail.com", Role.ADMINISTRADOR)));
        when(repo.countByRoleAndActivoTrue(Role.ADMINISTRADOR)).thenReturn(1L);
        assertThrows(BusinessRuleException.class, () -> service.cambiarEstado(5L, false, ADMIN_EMAIL));
    }

    //Consultas

    @Test
    @SuppressWarnings("unchecked")
    void listarLimitaElTamanoDePaginaYMapeaSinContrasena() {
        when(repo.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(user(1, "a@mail.com", Role.CLIENTE_EMISOR)),
                        inv.getArgument(1), 1));

        PageResponse<UsuarioResponse> page = service.listar(Role.CLIENTE_EMISOR, true, "ana", -3, 500);

        assertEquals(1, page.totalElements());
        assertEquals(0, page.page());
        assertEquals(UsuarioService.MAX_PAGE_SIZE, page.size());
        assertEquals("a@mail.com", page.content().get(0).email());
        verify(repo).findAll(any(Specification.class), eq(PageRequest.of(0, 50, org.springframework.data.domain.Sort.by("id"))));
    }

    @Test
    void obtenerUsuarioExistente() {
        when(repo.findById(2L)).thenReturn(Optional.of(user(2, "c@mail.com", Role.CLIENTE_EMISOR)));
        assertEquals(2L, service.obtener(2L).id());
    }

    @Test
    void obtenerUsuarioInexistente() {
        when(repo.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.obtener(2L));
    }

    //Crear usuario con rol

    @Test
    void crearRepartidorConContrasenaCifrada() {
        when(repo.existsByEmail("rep@mail.com")).thenReturn(false);

        UsuarioResponse res = service.crear(
                new CrearUsuarioRequest(" Pedro Rep ", "Rep@Mail.com", "Clave1234", Role.REPARTIDOR));

        assertEquals(Role.REPARTIDOR, res.role());
        assertEquals("rep@mail.com", res.email());
        verify(repo).save(argThat(u -> encoder.matches("Clave1234", u.getPasswordHash())));
    }

    @Test
    void crearConCorreoDuplicadoFalla() {
        when(repo.existsByEmail("rep@mail.com")).thenReturn(true);
        assertThrows(DuplicateEmailException.class,
                () -> service.crear(new CrearUsuarioRequest("Pedro", "rep@mail.com", "Clave1234", Role.REPARTIDOR)));
    }

    @Test
    void crearConCarreraDeCorreoDuplicadoFalla() {
        when(repo.existsByEmail(anyString())).thenReturn(false);
        when(repo.save(any(User.class))).thenThrow(new DataIntegrityViolationException("dup"));
        assertThrows(DuplicateEmailException.class,
                () -> service.crear(new CrearUsuarioRequest("Pedro", "rep@mail.com", "Clave1234", Role.REPARTIDOR)));
    }

    //Perfil propio
    @Test
    void verPerfilPropio() {
        when(repo.findByEmail("c@mail.com")).thenReturn(Optional.of(user(2, "c@mail.com", Role.CLIENTE_EMISOR)));
        assertEquals(Role.CLIENTE_EMISOR, service.perfil("c@mail.com").role());
    }

    @Test
    void actualizarPerfilNoCambiaCorreoNiRolYNormalizaVacios() {
        User u = user(2, "c@mail.com", Role.CLIENTE_EMISOR);
        u.setTelefono("3000000000");
        when(repo.findByEmail("c@mail.com")).thenReturn(Optional.of(u));

        UsuarioResponse res = service.actualizarPerfil("c@mail.com",
                new ActualizarPerfilRequest("  Ana Maria ", "  ", " Calle 10 # 5-20 "));

        assertEquals("Ana Maria", res.nombre());
        assertNull(res.telefono());
        assertEquals("Calle 10 # 5-20", res.direccion());
        assertEquals("c@mail.com", res.email());
        assertEquals(Role.CLIENTE_EMISOR, res.role());
    }

    @Test
    void perfilDeUsuarioInexistente() {
        when(repo.findByEmail("x@mail.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.perfil("x@mail.com"));
    }
}