# Entorno local con Docker y documentación Swagger

## Requisitos
- Docker Desktop instalado y abierto.

## Todo con un comando (PostgreSQL + backend)
1. Copia `.env.example` como `.env` y completa `JWT_SECRET` (el archivo explica cómo generarlo) y la clave de la base de datos.
2. Desde la raíz del repositorio:
   ```
   docker compose up --build
   ```
3. El backend queda en `http://localhost:8090` (o el puerto de `BACKEND_PORT`).
4. Para apagarlo: `docker compose down`. Para borrar también los datos: `docker compose down -v`.

## Solo la base de datos (mientras programas desde IntelliJ)
```
docker compose up -d db
```
En IntelliJ, en *Run → Edit Configurations → UqDeliveryApplication → Environment variables*:
```
SPRING_PROFILES_ACTIVE=prod;DB_URL=jdbc:postgresql://localhost:5432/uqdelivery;DB_USER=uqdelivery;DB_PASSWORD=<tu clave>;JWT_SECRET=<tu secreto>
```
Sin esas variables, la aplicación usa la base H2 en memoria de siempre.

## Swagger (documentación interactiva de la API)
- Con el backend corriendo: `http://localhost:8090/swagger-ui.html`
- JSON de la especificación: `http://localhost:8090/v3/api-docs`

Para probar un endpoint protegido: ejecuta `POST /api/auth/login`, copia el `token` de la respuesta, pulsa **Authorize** (arriba a la derecha) y pégalo.

Cada integrante documenta sus endpoints con `@Tag`, `@Operation` y `@ApiResponse` (ver `AuthController` como ejemplo).

## Notas
- El perfil `prod` usa `ddl-auto: update`. Cuando el equipo adopte Flyway, pasará a `validate`.
- Nunca subas el archivo `.env` al repositorio (debe estar en `.gitignore`).
