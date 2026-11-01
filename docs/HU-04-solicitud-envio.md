# HU-04 · Creación de solicitud de envío

Responsable: Miguel Ángel Betancourt Carmona · Rama: `feature/HU-04-solicitud-envio` · 8 puntos
Ampliación (búsqueda de lugares con TomTom): Jose Manuel Bedoya Mondragón · Rama: `feature/HU-04-05-busqueda-lugares`

> Como cliente emisor, quiero crear una solicitud de envío con los datos del paquete, el origen, el
> destino y el receptor, para que quede registrada y reciba un código de seguimiento único.

## Cómo correrlo

Igual que las demás historias: backend en http://localhost:8090 (`cd backend && mvn spring-boot:run`)
y frontend en http://localhost:5173 (`cd frontend && npm run dev`, proxy `/api` → 8090).
- Cliente emisor: registrarse en `/registro` → menú **Enviar paquete** (o "Solicitar un envío" en el inicio).
- Para buscar lugares hace falta la variable `TOMTOM_API_KEY` (ver *Búsqueda de lugares*).

## Flujo (asistente en pasos)

1. **Paquete y ubicación**: peso, medidas, las direcciones de origen y destino (el texto que lee el repartidor) y
   el buscador de **Origen** y **Destino** (reutiliza `QuoteForm` de HU-05), con un botón "Calcular precio".
   Al elegir un lugar de la lista el sistema llena las coordenadas; ya no se escriben latitud y longitud.
2. **Receptor**: nombre, teléfono, correo (opcional) y descripción del contenido (opcional).
3. **Resumen**: todo lo anterior más el precio estimado (`QuoteResult`) y el botón para confirmar.
4. **Confirmación**: código de seguimiento (`UQD-XXXXXXXX`) con botón de copiar.

## Búsqueda de lugares (TomTom)

```
Navegador ──GET /api/lugares?q=...──▶ Backend (Spring) ──▶ TomTom Search API
```

**Qué ve el usuario**
- Un cuadro por lugar (origen y destino) con **autocompletado**: busca cuando se deja de escribir. Sin selector de ciudad ni botón *Buscar*.
- Cada opción muestra el **nombre** del lugar y, debajo, su **calle y la ciudad**. No se muestra el barrio de TomTom: para
  Armenia devolvía "San Pedro" en casi todo y confundía. Se quitó también el ruido administrativo (comuna, perímetro, departamento).
- La dirección exacta que lee el repartidor es el texto que escribe el cliente; nunca se reemplaza por el nombre del mapa.
  Solo si ese campo estaba vacío se llena con lo que se buscó.
- Editar una dirección ya no borra el peso ni las medidas, y al cambiar cualquier dato se descarta el precio y hay que
  volver a pulsar *Calcular precio*.

**Lugares definidos por el equipo.** `PuntosConocidos` guarda sitios con coordenadas fijas. Si se escribe "Universidad del
Quindío" aparecen las dos entradas (Ingeniería y Ciencias Básicas) y no se consulta a TomTom. Para agregar uno: en Google
Maps, clic derecho sobre el sitio → clic en los números (latitud, longitud) → copiarlos en la lista.

**Por qué TomTom.** Gratis y sin tarjeta, y una sola clave da búsqueda con autocompletado y rutas por calles.

| Opción | Gratis | Tarjeta | Búsqueda + rutas | Nota |
|---|---|---|---|---|
| **TomTom** (elegida) | 20.000 búsquedas y 20.000 rutas / mes (según su página de precios) | No | Sí | Datos propios, no solo OpenStreetMap |
| Google Maps | 10.000 por servicio / mes | **Sí** | Sí | Mejor dato en Colombia; exige facturación activa |
| Geoapify | 3.000 créditos / día | No | Sí | Basada en OpenStreetMap (mismo problema de números de casa) |
| Nominatim (primer intento) | Sin clave | No | Solo búsqueda | No permite autocompletar; sin ruta |

El proveedor está aislado en `TomTomMapasClient`: cambiar a Google u otro es reemplazar esa clase.

**Puesta en marcha**
1. Crear cuenta en <https://developer.tomtom.com> y copiar la *API key*.
2. Definir `TOMTOM_API_KEY` antes de arrancar el backend: en PowerShell `$env:TOMTOM_API_KEY="tu-clave"`; en IntelliJ,
   *Run configuration → Environment variables*; con Docker, la línea `TOMTOM_API_KEY=...` del `.env`. **Nunca** va en el repositorio.
3. Probar en *Enviar paquete* o *Cotizar*: escribir `Universidad del Quindío` y otro lugar y elegir.

Sin clave, `/api/lugares` responde 503 con mensaje claro y la cotización sigue con línea recta.

**Configuración (`application.yml`)**

| Propiedad | Valor | Uso |
|---|---|---|
| `app.maps.tomtom-key` | `${TOMTOM_API_KEY:}` | Clave |
| `app.maps.centro-lat/lng` | Armenia | Centro de la búsqueda |
| `app.maps.ciudad` | `Armenia` | Solo se muestran resultados de este municipio |
| `app.maps.radio-metros` | `10000` | Limita a la zona urbana (10 km); `0` = solo prioridad, sin límite |

**Limitaciones conocidas**
- Los números de casa (`Cra 14 # 5-15`) casi nunca están en un mapa gratuito de Colombia: se ubica la calle o la zona. Para
  el precio basta, pero a veces TomTom ubica el punto a varias cuadras y la distancia por ruta difiere de Google Maps.
- Si un lugar importante no aparece o sale mal ubicado: agregarlo a `PuntosConocidos`, marcar el punto en un mapa (Sprint 2)
  o cambiar de proveedor.

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
| GET | `/api/lugares?q=` | Usuario autenticado | 200 (lista de lugares) · 401 · 503 (mapas sin clave o caídos) |

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
| `controller/LugarController.java` · `dto/LugarResponse.java` | `GET /api/lugares?q=` (nombre, calle/ciudad y coordenadas) |
| `service/TomTomMapasClient.java` | Llama a TomTom (búsqueda y ruta); no registra la clave |
| `service/LugarMapper.java` | Deja título corto y subtítulo calle · ciudad, sin ruido administrativo |
| `service/PuntosConocidos.java` | Lugares con coordenadas fijas (entradas de la Universidad del Quindío) |
| `exception/MapasNoDisponibleException.java` | Mapas sin clave o caídos → 503 |

## Frontend

| Archivo | Qué hace |
|---|---|
| `envioApi.js` | `crearEnvio`, `misEnvios`, `obtenerEnvio` (mismo estilo que `cotizacionApi.js`; 401 cierra sesión) |
| `envioValidation.js` (+ test) | Direcciones y datos del receptor; el paquete y las coordenadas los valida `QuoteForm` |
| `pages/NewShipment.jsx` | `/envios/nuevo` (solo CLIENTE_EMISOR): asistente en 3 pasos + confirmación |
| `envio.css` | Estilos del asistente (pasos) y la pantalla de confirmación |
| `lugaresApi.js` (+ test) | Normaliza el texto (`Cra`→`Carrera`, quita `#`) y llama al backend |
| `components/AddressSearch.jsx` · `geocodificacion.css` | Autocompletado con teclado (↑ ↓ Enter Esc) y lugar fijado |

## Cambios sobre archivos de otros integrantes (aditivos, commits `chore(HU-04)`)

| Archivo | Cambio | Motivo |
|---|---|---|
| `config/SecurityConfig.java` | + 1 regla `requestMatchers` | 403 antes que 400 para otros roles |
| `frontend/src/App.jsx` | + ruta `/envios/nuevo` | Pantalla nueva |
| `frontend/src/components/AppShell.jsx` | + enlace "Enviar paquete" (cliente) | Menú por rol |
| `frontend/src/pages/Home.jsx` | "Solicitar un envío" ahora enlaza a `/envios/nuevo` | Acceso desde el inicio |
| `exception/GlobalExceptionHandler.java` | + handler `MapasNoDisponibleException` → 503 | Mapas sin clave o caídos |
| `resources/application.yml` · `.env.example` | + bloque `app.maps` y variable `TOMTOM_API_KEY` | Configurar TomTom |
| `frontend/src/components/QuoteForm.jsx` · `QuoteResult.jsx` · `pages/Quote.jsx` · `pages/NewShipment.jsx` | Buscador de lugares en vez de latitud/longitud | Origen y destino por nombre |

## Supuestos y pendientes (para decidir en equipo)

- **Búsqueda por nombre, todavía sin mapa:** origen y destino se buscan con TomTom y se elige de una lista. El mapa para
  marcar el punto, que pide el enunciado, se deja para el Sprint 2.
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
- Búsqueda de lugares: `LugarControllerTest` (5), `LugarMapperTest` (6), `PuntosConocidosTest` (3).
- Frontend: `envioValidation.test.js` (Vitest, 12) y `lugaresApi.test.js` (7).
- Cobertura JaCoCo de `EnvioService`: 97 % de instrucciones, 83 % de ramas (`cd backend && mvn test`
  → `target/site/jacoco/index.html`); `EnvioController`: 100 %.
