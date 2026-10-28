# HU-01 · Registro e inicio de sesión seguro

Responsable: Jose Manuel Bedoya · Rama: `feature/HU-01-*` · 5 puntos

## Cómo correrlo
Backend (Java 17+, Maven):
    cd backend
    mvn spring-boot:run        # http://localhost:8080  (BD H2 en memoria por defecto)
    mvn test                   # pruebas + reporte de cobertura en target/site/jacoco/index.html
Frontend (Node 18+):
    cd frontend
    npm install
    npm run dev                # http://localhost:5173  (proxy /api -> :8080)
    npm test

Producción: perfil `prod` y variables `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (Base64, >= 32 bytes), `CORS_ORIGINS`.

## API
| Método | Ruta | Descripción |
|---|---|---|
| POST | /api/auth/register | Crea cuenta (siempre rol CLIENTE_EMISOR). 201 / 400 / 409 |
| POST | /api/auth/login | Devuelve JWT + rol. 200 / 400 / 401 / 429 |
| GET | /api/auth/me | Usuario de la sesión (requiere `Authorization: Bearer ...`). 200 / 401 |

## Criterios de aceptación → implementación
1. Datos válidos y no registrados → cuenta creada: `AuthService.register`, 201.
2. Credenciales incorrectas → acceso rechazado con mensaje: `InvalidCredentialsException`, 401, mensaje genérico.

## Seguridad aplicada
- Contraseñas con BCrypt; nunca se devuelven.
- Validación en cliente (`validation.js`) y servidor (Bean Validation).
- SQL injection: JPA con consultas parametrizadas. XSS: nombre restringido a letras, React escapa la salida, CSP `default-src 'none'` en la API.
- Mensaje de error genérico y comparación de hash también con correo inexistente (evita enumeración de usuarios).
- Bloqueo 15 min tras 5 intentos fallidos por correo.
- El rol no se acepta desde el cliente: nadie se registra como Administrador/Repartidor (eso lo hará el admin en HU-02).
- JWT firmado HS256 con expiración de 60 min; sesión en `sessionStorage`.

## Para Sara (HU-02) y Jose/Cesar (HU-03)
`User` y `Role` ya existen; para proteger endpoints por rol usar `@PreAuthorize("hasRole('ADMINISTRADOR')")`.

## Pruebas y cobertura
- Backend: 23 pruebas (JwtServiceTest 4, AuthServiceTest 8, AuthControllerTest 11). Reporte JaCoCo: `backend/target/site/jacoco/index.html`.
- Cobertura obtenida: __ % (completar con el reporte).
- Frontend: pruebas de validación con Vitest (`npm test`).
