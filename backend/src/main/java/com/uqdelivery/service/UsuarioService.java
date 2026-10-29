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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * HU-02 · Gestion de perfiles y roles.
 * Reglas de negocio:
 *  - Solo un ADMINISTRADOR gestiona roles y estados (se controla en SecurityConfig y con @PreAuthorize).
 *  - Un administrador no puede cambiar su propio rol ni desactivarse a si mismo.
 *  - Siempre debe quedar al menos un administrador activo.
 *  - El correo no se edita desde el perfil.
 */
@Service
public class UsuarioService {

    static final int MAX_PAGE_SIZE = 50;

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UsuarioService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    //Administracion

    @Transactional(readOnly = true)
    public PageResponse<UsuarioResponse> listar(Role rol, Boolean activo, String busqueda, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Specification<User> spec = Specification.where(conRol(rol))
                .and(conActivo(activo))
                .and(conBusqueda(busqueda));
        return PageResponse.of(
                users.findAll(spec, PageRequest.of(safePage, safeSize, Sort.by("id"))),
                UsuarioResponse::from);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.from(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw new DuplicateEmailException();
        User user = new User(req.nombre().trim(), email, encoder.encode(req.password()), req.role());
        try {
            return UsuarioResponse.from(users.save(user));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException();
        }
    }

    @Transactional
    public UsuarioResponse cambiarRol(Long id, Role nuevoRol, String emailAdmin) {
        User user = buscar(id);
        if (user.getEmail().equalsIgnoreCase(emailAdmin)) {
            throw new BusinessRuleException("No puedes cambiar tu propio rol");
        }
        if (user.getRole() == nuevoRol) {
            return UsuarioResponse.from(user);
        }
        if (esUltimoAdministradorActivo(user)) {
            throw new BusinessRuleException("Debe existir al menos un administrador activo");
        }
        user.setRole(nuevoRol);
        return UsuarioResponse.from(users.save(user));
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, boolean activo, String emailAdmin) {
        User user = buscar(id);
        if (!activo && user.getEmail().equalsIgnoreCase(emailAdmin)) {
            throw new BusinessRuleException("No puedes desactivar tu propia cuenta");
        }
        if (user.isActivo() == activo) {
            return UsuarioResponse.from(user);
        }
        if (!activo && esUltimoAdministradorActivo(user)) {
            throw new BusinessRuleException("Debe existir al menos un administrador activo");
        }
        user.setActivo(activo);
        return UsuarioResponse.from(users.save(user));
    }

    //Perfil propio

    @Transactional(readOnly = true)
    public UsuarioResponse perfil(String email) {
        return UsuarioResponse.from(buscarPorEmail(email));
    }

    @Transactional
    public UsuarioResponse actualizarPerfil(String email, ActualizarPerfilRequest req) {
        User user = buscarPorEmail(email);
        user.setNombre(req.nombre().trim());
        user.setTelefono(vacioANull(req.telefono()));
        user.setDireccion(vacioANull(req.direccion()));
        return UsuarioResponse.from(users.save(user));
    }

    //Auxiliares

    private User buscar(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private User buscarPorEmail(String email) {
        return users.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private boolean esUltimoAdministradorActivo(User user) {
        return user.getRole() == Role.ADMINISTRADOR && user.isActivo()
                && users.countByRoleAndActivoTrue(Role.ADMINISTRADOR) <= 1;
    }

    private static String vacioANull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static Specification<User> conRol(Role rol) {
        return (root, query, cb) -> rol == null ? null : cb.equal(root.get("role"), rol);
    }

    static Specification<User> conActivo(Boolean activo) {
        return (root, query, cb) -> activo == null ? null : cb.equal(root.get("activo"), activo);
    }

    /** Busqueda por nombre o correo. Criteria API = consulta parametrizada (sin riesgo de SQL injection). */
    static Specification<User> conBusqueda(String texto) {
        return (root, query, cb) -> {
            if (texto == null || texto.isBlank()) return null;
            String patron = "%" + texto.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.or(cb.like(cb.lower(root.get("nombre")), patron),
                    cb.like(cb.lower(root.get("email")), patron));
        };
    }
}