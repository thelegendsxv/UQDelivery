# HU-03 · Recuperación de contraseña

Responsable: César Santiago Bayona · Rama: `feature/HU-03-recuperar-password` · 3 puntos · Revisa: Sara

> Como usuario registrado, quiero recuperar mi contraseña mediante un mecanismo seguro, para restablecer el acceso
> a mi cuenta cuando la olvide.

## Cómo correrlo
Igual que HU-01 y HU-02: backend en http://localhost:8090 (`cd backend && mvn spring-boot:run`) y frontend en
http://localhost:5173 (`cd frontend && npm run dev`).
Sin `MAIL_HOST` configurado, el enlace de recuperación se imprime en la consola del backend
(línea `Enlace de recuperacion para ...`), así se puede probar el flujo sin servidor de correo.

## Archivos de la historia
Siguen la misma organización por capas del proyecto (`com.uqdelivery`), junto a los de HU-01 y HU-02:

| Capa | Archivo | Qué hace |
|---|---|---|
| `controller/` | `ReseteoContrasenaController` | Endpoints `forgot-password` y `reset-password` |
| `service/` | `ReseteoContrasenaService` | Genera, valida y consume el token; cambia la contraseña |
| `service/` | `ReseteoContrasenaEmailService` | Envía el correo con `JavaMailSender` (asíncrono) |
| `service/` | `TokenCleanupTask` | `@Scheduled`: borra tokens vencidos cada día |
| `repository/` | `ReseteoContrasenaTokenRepository` | Búsqueda por hash, tokens pendientes y borrado de vencidos |
| `domain/` | `ReseteoContrasenaToken` | Entidad de la tabla `password_reset_tokens` |
| `dto/` | `OlvidoContrasenaRequest`, `ReseteoContrasenaRequest`, `MessageResponse` | Entrada validada y respuesta |
| `config/` | `ModuloContrasenaConfig` | Bean `JavaMailSender` desde `MAIL_*`, `@EnableAsync`, `@EnableScheduling` |
| `exception/` | `ReseteoContrasenaException` | Error de negocio → 400 |

Archivos compartidos modificados: `config/SecurityConfig` (endpoints públicos), `exception/GlobalExceptionHandler`
(manejo de `ReseteoContrasenaException`), `exception/TooManyAttemptsException` (constructor con mensaje),
`pom.xml` (`spring-boot-starter-mail`), `application.yml` y `application-prod.yml`.

Migración: `resources/db/migration/V2__password_reset_tokens.sql` (versión V2 reservada en la guía del sprint).

## API

| Método | Ruta | Body | Respuestas |
|---|---|---|---|
| POST | `/api/auth/forgot-password` | `{"email"}` | 200 (siempre el mismo mensaje) · 400 · 429 |
| POST | `/api/auth/reset-password` | `{"token","password"}` | 200 · 400 (enlace inválido, vencido o usado; contraseña débil) |

Ambos son públicos (sin `Authorization`). Errores con el formato común: `{ "status", "message", "fieldErrors" }`.

El correo lleva el enlace `FRONTEND_URL/reset-password?token=<token>`. El frontend lee el `token` de la URL y lo envía
junto con la nueva contraseña a `/reset-password`.

## Criterios de aceptación → implementación

1. **Dado que un usuario solicita recuperar su contraseña con un correo registrado, cuando envía la solicitud,
   entonces el sistema inicia el proceso de recuperación.**
   `ReseteoContrasenaService.solicitarRecuperacion` genera el token, guarda su hash con vencimiento y envía el enlace
   con `ReseteoContrasenaEmailService`.
   Pruebas: `correoRegistradoGeneraTokenHasheadoYEnviaEnlace`, `correoInexistenteRecibeLaMismaRespuestaYNoSeEnviaNada`.
2. **Dado que el usuario presenta un mecanismo de recuperación válido, cuando establece una nueva contraseña,
   entonces el sistema la actualiza y permite iniciar sesión.**
   `ReseteoContrasenaService.restablecer` valida el token, guarda la nueva contraseña con BCrypt y marca el token como usado.
   Pruebas: `flujoCompletoPermiteIniciarSesionConLaNuevaContrasena`, `tokenValidoActualizaContrasenaConBCryptYMarcaUsado`.

## Modelo de datos

Tabla `password_reset_tokens` (relación `users` 1 ── N `password_reset_tokens`):

| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGSERIAL | PK |
| `usuario_id` | BIGINT | FK → `users.id`, `ON DELETE CASCADE` |
| `token_hash` | VARCHAR(64) | UNIQUE. Hash SHA-256 del token, nunca el token en claro |
| `fecha_expiracion` | TIMESTAMPTZ | 30 minutos después de la solicitud |
| `usado` | BOOLEAN | `false` por defecto |
| `fecha_creacion` | TIMESTAMPTZ | |

Justificación: guardar solo el hash evita que alguien con acceso a la BD use los enlaces; la FK garantiza que cada
token pertenezca a un usuario existente y el UNIQUE en `token_hash` evita duplicados.

## Seguridad aplicada

- Token aleatorio de 32 bytes con `SecureRandom` (Base64 URL, 43 caracteres). En BD solo se guarda su hash SHA-256.
- Vence a los **30 minutos** y es de **un solo uso**. Pedir un enlace nuevo invalida los anteriores, y cambiar la
  contraseña invalida todos los pendientes.
- **Sin enumeración de usuarios**: la respuesta es idéntica exista o no el correo («Si el correo existe, enviamos
  instrucciones…»), y el correo se envía con `@Async` para que el tiempo de respuesta tampoco lo delate. El mensaje
  es el mismo para un token inexistente, vencido o ya usado.
- Máximo **5 solicitudes por correo cada 15 minutos** (429). Reutiliza `LoginAttemptService` con el prefijo
  `reset:`, así que no bloquea el login.
- La nueva contraseña cumple la política del registro de HU-01 (8 a 72 caracteres, mayúscula, minúscula y número),
  debe ser distinta a la actual y se guarda con BCrypt.
- Al restablecer se desbloquea el login si estaba bloqueado por intentos fallidos.
- Las cuentas desactivadas (HU-02) no reciben enlace ni pueden usar uno ya emitido.
- Validación en servidor con Bean Validation (correo y formato del token). Consultas JPA parametrizadas.
- Credenciales SMTP solo en variables de entorno, nunca en el repositorio.
- `TokenCleanupTask` borra los tokens vencidos todos los días a las 3:00 a. m.

## Configuración

| Variable | Uso | Por defecto (dev) |
|---|---|---|
| `FRONTEND_URL` | Base del enlace del correo | `http://localhost:5173` |
| `MAIL_HOST`, `MAIL_PORT` | Servidor SMTP | vacío, `587` |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Credenciales SMTP | vacío |
| `MAIL_FROM` | Remitente | `no-reply@uqdelivery.com` |

- **Mailtrap (desarrollo):** `MAIL_HOST=sandbox.smtp.mailtrap.io`, `MAIL_PORT=2525`, usuario y clave del inbox.
- **Gmail:** `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, contraseña de aplicación.
- En el perfil `prod` el enlace nunca se escribe en el log, así que se requieren `MAIL_*` y `FRONTEND_URL`.

## Pruebas

| Clase | Ubicación | Casos |
|---|---|---|
| `ReseteoContrasenaServiceTest` | `test/.../service/` | 13: token válido, vencido, ya usado e inexistente; correo inexistente; cuenta desactivada; invalidación de tokens previos; límite de solicitudes; contraseña repetida; desbloqueo del login; hash SHA-256 |
| `ReseteoContrasenaEmailServiceTest` | `test/.../service/` | 3: mock de `JavaMailSender`, sin SMTP, fallo del SMTP |
| `TokenCleanupTaskTest` | `test/.../service/` | 1: borrado de vencidos |
| `ReseteoContrasenaControllerTest` | `test/.../controller/` | 9 (MockMvc): flujo completo hasta el login, misma respuesta para correo inexistente, token usado, enlace anterior invalidado, token inventado, contraseña débil, datos mal formados, 429, endpoints públicos |

Reporte de cobertura: `backend/target/site/jacoco/index.html` (después de `mvn test`).
Cobertura obtenida: __ % (completar con el reporte).

## Pendiente según la Definición de Hecho

- Swagger: documentar los 2 endpoints cuando se agregue `springdoc-openapi` al proyecto.
- Flyway: la migración V2 queda lista; se activa cuando se configure Flyway y `ddl-auto: validate`.
- Frontend: páginas `ForgotPassword` y `ResetPassword`, y enlace «¿Olvidaste tu contraseña?» en el Login.
