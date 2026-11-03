# HU-04 · Creación de solicitud de envío

Responsable: Miguel Ángel Betancourt Carmona · Rama: `feature/HU-04-solicitud-envio` · 8 puntos

> Como cliente emisor, quiero crear una solicitud de envío con los datos del paquete, el origen, el
> destino y el receptor, para que quede registrada y reciba un código de seguimiento único.

## Cómo correrlo

Igual que las demás historias: backend en http://localhost:8090 (`cd backend && mvn spring-boot:run`)
y frontend en http://localhost:5173 (`cd frontend && npm run dev`, proxy `/api` → 8090).
- Cliente emisor: registrarse en `/registro` → menú **Enviar paquete** (o "Solicitar un envío" en el inicio).

## Flujo (asistente en pasos)

1. **Paquete y ubicación**: peso, medidas y coordenadas de origen/destino (reutiliza `QuoteForm` de
   HU-05) + las direcciones de origen y destino, con un botón "Calcular precio".
2. **Receptor**: nombre, teléfono, correo (opcional) y descripción del contenido (opcional).
3. **Resumen**: todo lo anterior más el precio estimado (`QuoteResult`) y el botón para confirmar.
4. **Confirmación**: código de seguimiento (`UQD-XXXXXXXX`) con botón de copiar.

## El precio y la distancia los calcula el servidor, no el cliente

`EnvioService.crear` arma un `CotizacionRequest` con los mismos datos del paquete y las coordenadas
y llama a `CotizacionService.cotizar` (HU-05) para obtener `distanciaKm` y `precioCotizado`.
`CrearEnvioRequest` ni siquiera tiene un campo de precio, así que no hay forma de que el cliente lo
manipule. Si no hay tarifa activa, se propaga el mismo 503 de HU-05.

## Código de seguimiento

`UQD-` + 8 caracteres en mayúscula de un alfabeto sin `O`, `0`, `I`, `1` (para que se lea y se dicte
fácil), con reintento (hasta 10 veces) si ya existe. Se verifica la unicidad contra la base de datos
antes de guardar.

## Estados y trazabilidad

`EstadoEnvio`: `PENDIENTE_ASIGNACION` (inicial) → `ASIGNADO` → `EN_RUTA` → `ENTREGADO` | `CANCELADO` |
`CON_NOVEDAD` (estos últimos y sus transiciones los usarán HU-06, 08, 11, 12 y 13). Cada envío crea su
primer registro en `historial_estado_envio` (con `estado_anterior = null`) al momento de solicitarse.

> **Aviso para el equipo:** `Envio.pesoKg` cambió de `Double` a `BigDecimal` (nadie más lo usaba
> todavía) para ser consistente con `CotizacionRequest`/`Tarifa`. El valor `PENDIENTE` del enum
> `EstadoEnvio` se renombró a `PENDIENTE_ASIGNACION` y se agregó `CON_NOVEDAD`, según el DER de la
> sección 5 de la guía. Comentarlo en el Daily: HU-06/08/11/12/13 usan este mismo enum.

## API

| Método | Ruta | Rol | Respuestas |
|---|---|---|---|
| POST | `/api/envios` | CLIENTE_EMISOR | 201 · 400 (datos pendientes) · 401 · 403 · 503 (sin tarifa activa) |
| GET | `/api/envios/mis-envios` | CLIENTE_EMISOR | 200 (solo los del cliente autenticado) |
| GET | `/api/envios/{codigo}` | CLIENTE_EMISOR | 200 · 401 · 403 · 404 (no existe o es de otro cliente) |

Errores con el formato común: `{ "status", "message", "fieldErrors": { "campo": "detalle" } }`.
Documentado en Swagger (http://localhost:8090/swagger-ui.html), etiqueta **Envíos**.

### POST /api/envios

Request:
```json
{ "pesoKg": 2, "largoCm": 30, "anchoCm": 20, "altoCm": 10,
  "origenDireccion": "Calle 1 # 2-3", "origenLat": 4.5339, "origenLng": -75.6811,
  "destinoDireccion": "Calle 9 # 10-11", "destinoLat": 4.5450, "destinoLng": -75.6620,
  "receptorNombre": "Juan Perez", "receptorTelefono": "3001234567", "receptorEmail": "juan@mail.com",
  "descripcion": "Documentos", "esFragil": true, "esUrgente": false }
```

Respuesta 201 (probada en vivo contra PostgreSQL real, no solo H2):
```json
{ "id": 1, "codigoSeguimiento": "UQD-M2Y4HRZP", "estado": "PENDIENTE_ASIGNACION",
  "pesoKg": 2.00, "largoCm": 30.00, "anchoCm": 20.00, "altoCm": 10.00,
  "origenDireccion": "Calle 1 # 2-3", "origenLat": 4.5339, "origenLng": -75.6811,
  "destinoDireccion": "Calle 9 # 10-11", "destinoLat": 4.545, "destinoLng": -75.662,
  "receptorNombre": "Juan Perez", "receptorTelefono": "3001234567", "receptorEmail": "juan@mail.com",
  "descripcion": "Documentos", "esFragil": true, "esUrgente": false,
  "distanciaKm": 2.45, "precioCotizado": 12540.00, "createdAt": "2026-10-30T15:45:48.749295Z" }
```

Respuesta 400 (criterio 2: datos pendientes y **sin crear el registro**):
```json
{ "status": 400, "message": "Datos invalidos",
  "fieldErrors": { "largoCm": "El largo es obligatorio", "receptorNombre": "El nombre del receptor es obligatorio" } }
```

## Criterios de aceptación → implementación

1. **Con todos los datos, el sistema registra el envío con un código de seguimiento único y el
   precio estimado.** `EnvioService.crear` + `CotizacionService.cotizar` (HU-05).
   Pruebas: `creaElEnvioConElPrecioYLaDistanciaDeLaCotizacion`, `guardaElEnvioYElPrimerRegistroDeHistorial`,
   `clienteCreaUnEnvioYRecibeElCodigoYElPrecio`.
2. **Si falta un dato, se indica qué falta y no se crea ningún registro.** Bean Validation en
   `CrearEnvioRequest` + `GlobalExceptionHandler` → 400 con `fieldErrors`; la validación corre antes
   de tocar la base de datos (ni el envío ni el historial se guardan).
   Pruebas: `sinDatosDevuelve400YNoCreaNingunRegistro`, `siNoHayTarifaActivaNoSeCreaNingunRegistro`.

## Seguridad

- RBAC con doble barrera (igual que HU-02/05): regla de URL en `SecurityConfig` + `@PreAuthorize` en
  el controlador, para que otro rol reciba 403 antes de que se valide el cuerpo.
- `GET /api/envios/{codigo}` solo devuelve el envío si pertenece al cliente autenticado; si no, 404
  (no revela si el código existe, mismo criterio que HU-03 con el correo).
- Nombre del receptor y descripción rechazan `<`/`>` (XSS); teléfono y correo con los mismos
  patrones que HU-02.
- El precio y la distancia se calculan siempre en el servidor (ver arriba): nunca llegan del cliente.

## Backend

| Archivo | Qué hace |
|---|---|
| `domain/Envio.java` | Entidad compartida, extendida con dimensiones, receptor, banderas y el resultado de la cotización |
| `domain/EstadoEnvio.java` | Enum compartido, con los 6 estados del DER (sección 5) |
| `domain/HistorialEstadoEnvio.java` | Trazabilidad de cambios de estado |
| `repository/EnvioRepository.java` · `HistorialEstadoEnvioRepository.java` | Consultas derivadas, sin SQL concatenado |
| `service/EnvioService.java` | Código único con reintento, llamada a `CotizacionService`, historial inicial, consultas propias |
| `controller/EnvioController.java` | Endpoints + Swagger |
| `dto/CrearEnvioRequest.java` · `EnvioResponse.java` | DTO validados; el controlador nunca devuelve la entidad |

## Frontend

| Archivo | Qué hace |
|---|---|
| `envioApi.js` | `crearEnvio`, `misEnvios`, `obtenerEnvio` (mismo estilo que `cotizacionApi.js`; 401 cierra sesión) |
| `envioValidation.js` (+ test) | Direcciones y datos del receptor; el paquete y las coordenadas los valida `QuoteForm` |
| `pages/NewShipment.jsx` | `/envios/nuevo` (solo CLIENTE_EMISOR): asistente en 3 pasos + confirmación |
| `envio.css` | Estilos del asistente (pasos) y la pantalla de confirmación |

## Cambios sobre archivos de otros integrantes (aditivos, commits `chore(HU-04)`)

| Archivo | Cambio | Motivo |
|---|---|---|
| `config/SecurityConfig.java` | + 1 regla `requestMatchers` | 403 antes que 400 para otros roles |
| `frontend/src/App.jsx` | + ruta `/envios/nuevo` | Pantalla nueva |
| `frontend/src/components/AppShell.jsx` | + enlace "Enviar paquete" (cliente) | Menú por rol |
| `frontend/src/pages/Home.jsx` | "Solicitar un envío" ahora enlaza a `/envios/nuevo` | Acceso desde el inicio |

## Supuestos y pendientes (para decidir en equipo)

- **Coordenadas a mano, no con mapa:** igual que HU-05 (ver `docs/HU-05-cotizacion.md`), el selector
  con Leaflet/Nominatim que pide el enunciado se deja para el Sprint 2.
- **Flyway nunca se configuró.** La guía lo asigna por persona (V1 Jose, V2 César, V3 Santiago,
  **V4 Miguel**) con `ddl-auto: validate`; el equipo sigue en `ddl-auto: update` (Santiago ya lo había
  dejado como supuesto en HU-05). Sigue pendiente porque pasar a `validate` afecta el esquema de
  todas las historias, no solo esta; se decide en equipo antes de hacerlo.
- **Despliegue de PostgreSQL en la nube (Render/Railway, día 7 de la guía) todavía no se hizo.**
  Se verificó la conexión real contra un Postgres levantado localmente con Docker
  (`docker compose up -d db`, perfil `prod`): las tablas, las llaves foráneas, el `CHECK` del enum y
  el primer registro de historial se crean correctamente. Falta crear el servicio en la nube y
  apuntar `DB_URL`/`DB_USER`/`DB_PASSWORD` ahí (y correr el flujo completo contra ese servicio antes
  del día 9).

## Pruebas

- `EnvioServiceTest` (Mockito, 10): usa un `CotizacionService` real con `TarifaRepository` y
  `DistanciaService` mockeados (igual que `CotizacionServiceTest`), para comprobar que el precio y
  la distancia salen de ahí y no del cliente. Historial inicial, reintento de código, sin tarifa
  activa, consulta propia vs. ajena.
- `EnvioControllerTest` (MockMvc, 12): 201, 400 sin crear registro, 503, 403 (REPARTIDOR,
  ADMINISTRADOR), 401, dueño vs. otro cliente en `mis-envios` y en `/{codigo}`, Swagger.
- Frontend: `envioValidation.test.js` (Vitest, 12).
- Cobertura JaCoCo de `EnvioService`: 97 % de instrucciones, 83 % de ramas (`cd backend && mvn test`
  → `target/site/jacoco/index.html`); `EnvioController`: 100 %.
