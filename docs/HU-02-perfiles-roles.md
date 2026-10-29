# HU-02 · Gestión de perfiles y roles

Responsable: Sara Sofía Bolaños · Ramas: `feature/HU-02-perfiles-roles` (backend) y `feature/HU-02-frontend` · 3 puntos

## Cómo correrlo
Igual que HU-01: backend en http://localhost:8090 (`cd backend && mvn spring-boot:run`) y frontend en
http://localhost:5173 (`cd frontend && npm run dev`, proxy `/api` → 8090).
Entrar con el administrador de desarrollo `admin@uqdelivery.com` / `Admin12345` → menú **Usuarios**.

> Como administrador del sistema, quiero gestionar los perfiles y roles de los usuarios, para controlar sus permisos
> de acceso según sus responsabilidades.

## API

| Método | Ruta | Rol | Respuestas |
|---|---|---|---|
| GET | `/api/usuarios?rol=&activo=&q=&page=&size=` | ADMINISTRADOR | 200 · 400 (filtro inválido) · 401 · 403 |
| GET | `/api/usuarios/{id}` | ADMINISTRADOR | 200 · 404 |
| POST | `/api/usuarios` (crear usuario con rol, p. ej. REPARTIDOR) | ADMINISTRADOR | 201 · 400 · 409 |
| PUT | `/api/usuarios/{id}/rol` · body `{"role":"REPARTIDOR"}` | ADMINISTRADOR | 200 · 400 · 404 · 409 |
| PATCH | `/api/usuarios/{id}/estado` · body `{"activo":false}` | ADMINISTRADOR | 200 · 404 · 409 |
| GET | `/api/usuarios/me` | Autenticado | 200 · 401 |
| PUT | `/api/usuarios/me` · body `{"nombre","telefono","direccion"}` | Autenticado | 200 · 400 |

Errores con el formato común: `{ "status", "message", "fieldErrors": { "campo": "detalle" } }`.

## Criterios de aceptación → implementación

1. **El administrador asigna o modifica el rol y se refleja en el perfil.** `UsuarioService.cambiarRol` guarda el
   cambio; `GET /api/usuarios/me` lo muestra. Como `JwtAuthFilter` lee el rol desde la BD en cada petición, el
   cambio aplica de inmediato aunque el usuario tenga un token anterior.
   Pruebas: `adminCambiaRolYSeReflejaEnElPerfilDelUsuario`, `elNuevoRolAplicaDeInmediatoSobreUnTokenAnterior`.
2. **Acceso a una funcionalidad que no corresponde al rol → denegado.** Doble barrera en el servidor: regla de URL en
   `SecurityConfig` + `@PreAuthorize("hasRole('ADMINISTRADOR')")`. Respuesta 403 en JSON. En el frontend,
   `Protected` (en `App.jsx`) acepta `roles` y redirige a `/acceso-denegado`; si la API responde 403, `AdminUsers`
   también lleva a esa pantalla. El menú (`AppShell.jsx`) oculta "Usuarios" a quien no es administrador.
   Pruebas: `clienteEmisorRecibe403AlListarUsuarios`, `repartidorRecibe403AlCambiarRoles`, `clienteRecibe403AlCambiarEstadoOCrearUsuarios`.

## Reglas de negocio

- Un administrador no puede cambiar su propio rol ni desactivar su propia cuenta.
- Siempre debe quedar al menos un administrador activo.
- El correo no se edita desde el perfil; el rol tampoco (solo el administrador).
- Un usuario desactivado no puede iniciar sesión y su token deja de servir de inmediato.

## Seguridad

- RBAC aplicado en el backend (no solo ocultando botones).
- Validación en cliente (`usuariosValidation.js`) y servidor (Bean Validation); se rechazan `<` y `>` en nombre y dirección (XSS).
- Búsqueda con Criteria API (consultas parametrizadas, sin concatenar SQL).
- Las respuestas nunca incluyen la contraseña ni su hash.

## Administrador inicial

`AdminSeeder` crea un ADMINISTRADOR al arrancar si no existe. En desarrollo: `admin@uqdelivery.com` / `Admin12345`.
En producción se definen `ADMIN_EMAIL` y `ADMIN_PASSWORD` (sin valores por defecto).

## Frontend

| Archivo | Qué hace |
|---|---|
| `usuariosApi.js` | Llamadas a `/api/usuarios` con el token de `auth.js` (mismo estilo que `api.js`) |
| `roles.js` | Nombres de los roles para mostrar |
| `usuariosValidation.js` (+ test) | Validación de perfil y de nuevo usuario |
| `usuarios.css` | Estilos de las pantallas nuevas, con los colores de `index.css` |
| `components/AppShell.jsx` | Barra superior con menú según el rol |
| `components/SelectField.jsx` | Select con el mismo aspecto que `Field` |
| `pages/AdminUsers.jsx` | Panel del administrador |
| `pages/Profile.jsx` | Mi perfil |
| `pages/Forbidden.jsx` | Acceso denegado |

## Cambios sobre archivos de otros integrantes (avisados al equipo)

| Archivo | Cambio | Motivo |
|---|---|---|
| `frontend/src/App.jsx` (Jose) | Rutas `/perfil`, `/admin/usuarios`, `/acceso-denegado`; `Protected` acepta `roles` | Criterio 2 en el frontend |
| `frontend/src/pages/Home.jsx` (Jose) | Botón "Mi perfil" y "Gestionar usuarios" con enlace | Acceso a las pantallas nuevas |
| `domain/User.java` (Jose) | + `telefono`, `direccion`, `activo`, `updatedAt` | Datos de perfil y desactivación |
| `repository/UserRepository.java` | + `JpaSpecificationExecutor`, `countByRoleAndActivoTrue` | Filtros y regla del último admin |
| `security/JwtAuthFilter.java` | Lee rol/estado desde la BD | El cambio de rol aplica de inmediato |
| `config/SecurityConfig.java` | Reglas `/api/usuarios/**` + `AccessDeniedHandler` 403 JSON | Criterio 2 |
| `service/AuthService.java` | Rechaza login de cuentas inactivas | Desactivación |
| `exception/GlobalExceptionHandler.java` | 404, 409, 403 y parámetro inválido | Respuestas uniformes |

## Pruebas

- `UsuarioServiceTest` (Mockito, 20 pruebas): reglas de negocio de rol, estado, creación y perfil.
- `UsuarioControllerTest` (MockMvc, 17 pruebas): 200 para ADMINISTRADOR, 403 para los demás roles, 401 sin token, validaciones.
- Frontend: `usuariosValidation.test.js` (Vitest, 5 pruebas).
- Ejecutar: `cd backend && mvn test` → reporte en `backend/target/site/jacoco/index.html`.

## Demostración (guion)

1. Entrar como `admin@uqdelivery.com` → menú **Usuarios**.
2. Filtrar por rol, buscar por nombre o correo.
3. Cambiar el rol de un cliente a **Repartidor** → mensaje de éxito.
4. Entrar como ese usuario → **Mi perfil** muestra el rol nuevo.
5. Como cliente, abrir `/admin/usuarios` → pantalla **Acceso denegado** (y la API responde 403).
6. Desactivar un usuario → ya no puede iniciar sesión.
7. Crear el **repartidor de prueba** desde "+ Nuevo usuario" (preparación para HU-06).