# HU-05 · Cotización automática de envíos

Responsable: Santiago Arias Rodríguez · Rama: `feature/HU-05-cotizacion` · 5 puntos

> Como cliente emisor, quiero conocer el precio estimado de mi envío antes de solicitarlo, para decidir si lo envío.

## Cómo correrlo
Igual que HU-01: backend en http://localhost:8090 (`cd backend && mvn spring-boot:run`) y frontend en
http://localhost:5173 (`cd frontend && npm run dev`, proxy `/api` → 8090).
- Cliente emisor: registrarse en `/registro` → menú **Cotizar** (o "Cotizar un envío" en el inicio).
- Administrador: `admin@uqdelivery.com` / `Admin12345` → menú **Tarifas**.

## Reglas tarifarias

```
precio = tarifa base + distancia_km × costo por km + peso facturable × costo por kg + recargos

peso volumétrico = largo_cm × ancho_cm × alto_cm / 5000
peso facturable  = max(peso real, peso volumétrico)
recargos         = recargo frágil (si es frágil) + recargo urgente (si es urgente)
```

- Distancia: línea recta con la fórmula de Haversine (radio 6371 km). Interfaz `DistanciaService`; la implementación
  del Sprint 1 es `HaversineDistanciaService`. En el Sprint 2 se reemplaza por la distancia por ruta de la API de mapas
  registrando otra implementación con `@Primary`, sin cambiar `CotizacionService`.
- Todo el cálculo usa `BigDecimal` (nunca `double` para dinero). Distancia, pesos, cada costo y el total se redondean
  a 2 decimales con `RoundingMode.HALF_UP`.
- Se usa siempre la tarifa activa (solo puede haber una). Sin tarifa activa → 503 con mensaje claro.
- La respuesta siempre trae `estimado: true`: es un valor estimado, no se guarda nada.
- `porcentajeSeguro` existe en la tarifa, pero **todavía no se aplica**: el envío no tiene valor declarado en el Sprint 1.

### Tarifa inicial (creada por `TarifaSeeder` si la tabla está vacía)

| Campo | Valor (COP) |
|---|---:|
| Tarifa base | 5 000 |
| Costo por km | 1 200 |
| Costo por kg facturable | 800 |
| Recargo frágil | 3 000 |
| Recargo urgente | 5 000 |
| Porcentaje de seguro | 1 % (reservado) |

### Ejemplos calculados (con la tarifa inicial)

| # | Paquete | Distancia | Peso real / volumétrico → facturable | Cálculo | Total |
|---|---|---:|---|---|---:|
| 1 | 3 kg, 40×30×20 cm, frágil · centro de Armenia (4.5339, −75.6811) → (4.5450, −75.6620) | 2,45 km | 3,00 / 4,80 → **4,80** | 5000 + 2,45×1200 (2940) + 4,80×800 (3840) + 3000 | **$14 780** |
| 2 | 5 kg, 10×10×10 cm, urgente | 10 km | 5,00 / 0,20 → **5,00** | 5000 + 12000 + 4000 + 5000 | **$26 000** |
| 3 | 2 kg, 30×20×10 cm, frágil + urgente, mismo punto | 0 km | 2,00 / 1,20 → **2,00** | 5000 + 0 + 1600 + 3000 + 5000 | **$14 600** |
| 4 | Redondeo: 0,5 kg, 33,3×10×10 cm; tarifa 1200,55/km y 799,99/kg | 3,33 km | 0,50 / 0,666→0,67 → **0,67** | 5000 + 3997,8315→3997,83 + 535,9933→535,99 | **$9 533,82** |

Los ejemplos 1 a 4 son pruebas automáticas (`CotizacionControllerTest` y `CotizacionServiceTest`); el 1 se verificó a mano con el backend corriendo.

## API

| Método | Ruta | Rol | Respuestas |
|---|---|---|---|
| POST | `/api/cotizaciones` | CLIENTE_EMISOR | 200 · 400 (datos pendientes) · 401 · 403 · 503 (sin tarifa activa) |
| GET | `/api/tarifas` | ADMINISTRADOR | 200 · 401 · 403 |
| PUT | `/api/tarifas/{id}` | ADMINISTRADOR | 200 · 400 · 401 · 403 · 404 · 409 (dejaría el sistema sin tarifa activa) |

Errores con el formato común: `{ "status", "message", "fieldErrors": { "campo": "detalle" } }`.
Todos los endpoints están documentados en Swagger (http://localhost:8090/swagger-ui.html), etiquetas **Cotizaciones** y **Tarifas**.

### POST /api/cotizaciones

Request (`esFragil` y `esUrgente` son opcionales; si no vienen, se toman como `false`):
```json
{ "pesoKg": 3, "largoCm": 40, "anchoCm": 30, "altoCm": 20,
  "origenLat": 4.5339, "origenLng": -75.6811, "destinoLat": 4.5450, "destinoLng": -75.6620,
  "esFragil": true, "esUrgente": false }
```

| Campo | Regla |
|---|---|
| `pesoKg`, `largoCm`, `anchoCm`, `altoCm` | obligatorios, > 0, ≤ 10000 |
| `origenLat`, `destinoLat` | obligatorios, entre −90 y 90 |
| `origenLng`, `destinoLng` | obligatorios, entre −180 y 180 |

Respuesta 200:
```json
{ "tarifa": "Tarifa urbana estandar", "tarifaBase": 5000.00,
  "distanciaKm": 2.45, "costoPorKm": 1200.00, "costoDistancia": 2940.00,
  "pesoRealKg": 3.00, "pesoVolumetricoKg": 4.80, "pesoFacturableKg": 4.80,
  "costoPorKg": 800.00, "costoPeso": 3840.00,
  "recargos": [ { "concepto": "Fragil", "valor": 3000.00 } ], "totalRecargos": 3000.00,
  "total": 14780.00, "moneda": "COP", "estimado": true }
```

Respuesta 400 (criterio 2: datos pendientes y **sin precio**):
```json
{ "status": 400, "message": "Datos invalidos",
  "fieldErrors": { "largoCm": "El largo es obligatorio", "origenLat": "La latitud de origen es obligatoria" } }
```

### GET /api/tarifas · PUT /api/tarifas/{id}

```json
{ "nombre": "Tarifa urbana estandar", "tarifaBase": 5000, "costoPorKm": 1200, "costoPorKg": 800,
  "recargoFragil": 3000, "recargoUrgente": 5000, "porcentajeSeguro": 1, "activa": true }
```
- Valores no negativos con máximo 2 decimales; `porcentajeSeguro` ≤ 100; `nombre` obligatorio, ≤ 60 caracteres, sin `<` ni `>`.
- `activa` es opcional: `true` activa esta tarifa y desactiva las demás; `false` sobre la tarifa activa → 409.

## Criterios de aceptación → implementación

1. **Con los datos del paquete y las ubicaciones, el sistema calcula el precio estimado.** `CotizacionService.cotizar`
   aplica la fórmula con la tarifa activa y devuelve el desglose con `estimado: true`. En el frontend, `QuoteResult`
   muestra el desglose con formato COP y la etiqueta "Valor estimado".
   Pruebas: `CotizacionServiceTest` (12), `clienteObtieneElDesgloseDelPrecioEstimado`, `clienteConTokenRealPuedeCotizarSinCasillas`.
2. **Si falta un dato, se indica qué falta y no se muestra precio.** Bean Validation en `CotizacionRequest` +
   `GlobalExceptionHandler` → 400 con `fieldErrors` y sin `total`. `QuoteForm` valida lo mismo antes de enviar
   (`cotizacionValidation.js`), marca cada campo y muestra la lista de datos pendientes; si el servidor responde 400,
   muestra sus `fieldErrors` en los mismos campos.
   Pruebas: `sinDatosDevuelve400ConLosCamposPendientesYSinPrecio`, `valoresFueraDeRangoDevuelven400`, `CotizacionRequestValidationTest`.

## Seguridad

- RBAC en el servidor con doble barrera (igual que HU-02): regla de URL en `SecurityConfig` + `@PreAuthorize` en cada controlador.
  La regla de URL hace que otro rol reciba 403 antes de que se valide el cuerpo (no 400).
- Consultas solo con Spring Data (consulta derivada y JPQL con `@Param`), sin concatenar SQL.
- Entradas numéricas con rangos; el nombre de la tarifa rechaza `<` y `>` (XSS).
- Los controladores devuelven DTO (`CotizacionResponse`, `TarifaResponse`), nunca la entidad.

## Backend

| Archivo | Qué hace |
|---|---|
| `domain/Tarifa.java` | Entidad `tarifas` (valores `NUMERIC(12,2)`, `porcentaje_seguro NUMERIC(5,2)`) |
| `repository/TarifaRepository.java` | Tarifa activa, listado y `desactivarOtras` (JPQL parametrizado) |
| `config/TarifaSeeder.java` | Crea la tarifa inicial si la tabla está vacía |
| `service/DistanciaService.java` · `HaversineDistanciaService.java` | Distancia en km (interfaz reemplazable) |
| `service/CotizacionService.java` | Fórmula y desglose |
| `service/TarifaService.java` | Edición de tarifas, una sola activa |
| `controller/CotizacionController.java` · `TarifaController.java` | Endpoints + Swagger |
| `dto/CotizacionRequest/Response`, `TarifaRequest/Response` | DTO validados |
| `exception/TarifaNoDisponibleException.java` | Sin tarifa activa → 503 |

## Frontend

| Archivo | Qué hace |
|---|---|
| `cotizacionApi.js` | `cotizar`, `listarTarifas`, `actualizarTarifa` (mismo estilo que `usuariosApi.js`; 401 cierra la sesión) |
| `cotizacionValidation.js` (+ test) | Mismas reglas que el servidor; acepta coma o punto decimal |
| `components/QuoteForm.jsx` | Formulario reutilizable (ver props abajo) |
| `components/QuoteResult.jsx` | Desglose con formato COP y "Valor estimado"; exporta `formatearCOP` |
| `pages/Quote.jsx` | `/cotizar` (solo CLIENTE_EMISOR) |
| `pages/AdminTarifas.jsx` | `/admin/tarifas` (solo ADMINISTRADOR): ver y editar la tarifa activa |
| `cotizacion.css` | Estilos, con los colores de `index.css` (lo importan los componentes) |

### Reutilizar `QuoteForm` y `QuoteResult` (p. ej. en el formulario de envío de HU-04)

| Prop de `QuoteForm` | Uso |
|---|---|
| `initialValues` | `{ pesoKg, largoCm, anchoCm, altoCm, origenLat, origenLng, destinoLat, destinoLng, esFragil, esUrgente }` (parcial). Si cambian, se aplican |
| `onResult(cotizacion, datos)` | Respuesta del servidor y cuerpo enviado |
| `onError(err)` | `err.status`, `err.message`, `err.fieldErrors` |
| `onChange(values)` | Cada cambio de campo |
| `onQuote(datos)` | Reemplaza la llamada por defecto a `POST /api/cotizaciones` (debe devolver una promesa) |
| `embedded` | `true` dentro de otro `<form>`: se dibuja como `<div>` y el botón es `type="button"` |
| `submitLabel` | Texto del botón (por defecto "Cotizar") |

```jsx
const [cotizacion, setCotizacion] = useState(null)

<form onSubmit={crearEnvio}>
  {/* ...campos propios del envío... */}
  <QuoteForm embedded submitLabel="Calcular precio"
             initialValues={{ pesoKg: envio.pesoKg, origenLat: envio.origenLat, origenLng: envio.origenLng }}
             onResult={(c, datos) => { setCotizacion(c); setEnvio((e) => ({ ...e, ...datos, precioCotizado: c.total })) }} />
  <QuoteResult cotizacion={cotizacion} />
</form>
```

## Cambios sobre archivos de otros integrantes (aditivos, commits `chore(HU-05)`)

| Archivo | Cambio | Motivo |
|---|---|---|
| `config/SecurityConfig.java` | + 2 reglas `requestMatchers` | 403 antes que 400 para otros roles |
| `exception/GlobalExceptionHandler.java` | + handler `TarifaNoDisponibleException` → 503 | Mismo formato de error |
| `frontend/src/App.jsx` | + rutas `/cotizar` y `/admin/tarifas` | Pantallas nuevas |
| `frontend/src/components/AppShell.jsx` | + enlaces "Cotizar" (cliente) y "Tarifas" (admin) | Menú por rol |
| `frontend/src/pages/Home.jsx` | + acciones "Cotizar un envío" y "Configurar tarifas" | Acceso desde el inicio |

## Supuestos

- El proyecto no usa Flyway (`ddl-auto: update`), así que no hay `V3__tarifas.sql`: la tabla la crea Hibernate a
  partir de `Tarifa` y la tarifa inicial la crea `TarifaSeeder`. Si el equipo adopta Flyway, el script equivalente es:
  ```sql
  CREATE TABLE tarifas (
    id BIGSERIAL PRIMARY KEY, nombre VARCHAR(60) NOT NULL,
    tarifa_base NUMERIC(12,2) NOT NULL, costo_por_km NUMERIC(12,2) NOT NULL, costo_por_kg NUMERIC(12,2) NOT NULL,
    recargo_fragil NUMERIC(12,2) NOT NULL, recargo_urgente NUMERIC(12,2) NOT NULL,
    porcentaje_seguro NUMERIC(5,2) NOT NULL, activa BOOLEAN NOT NULL);
  INSERT INTO tarifas (nombre, tarifa_base, costo_por_km, costo_por_kg, recargo_fragil, recargo_urgente, porcentaje_seguro, activa)
  VALUES ('Tarifa urbana estandar', 5000, 1200, 800, 3000, 5000, 1.00, TRUE);
  ```
- Los nombres JSON siguen los de `Envio` (camelCase: `pesoKg`, `origenLat`...). Los campos `largoCm`, `anchoCm`,
  `altoCm`, `esFragil`, `esUrgente`, `distanciaKm` y `precioCotizado` todavía no están en `Envio` (son de HU-04).
- Las coordenadas se escriben a mano en el Sprint 1; el mapa llega en el Sprint 2.
- Los recargos son valores fijos en COP, no porcentajes.

## Pruebas

- `CotizacionServiceTest` (Mockito, 12): volumétrico > real, real > volumétrico, frágil, urgente, ambos, distancia 0, redondeo HALF_UP, sin tarifa activa.
- `HaversineDistanciaServiceTest` (4), `TarifaServiceTest` (6), `CotizacionRequestValidationTest` (5, datos faltantes y rangos).
- `CotizacionControllerTest` (MockMvc, 11): 200, 400 sin precio, 503, 403 (REPARTIDOR, ADMINISTRADOR), 401, Swagger.
- `TarifaControllerTest` (MockMvc, 9): 200 ADMINISTRADOR, 400, 404, 409, 403 (CLIENTE_EMISOR, REPARTIDOR), 401.
- Frontend: `cotizacionValidation.test.js` (Vitest, 8).
- Cobertura JaCoCo de las clases de HU-05: 100 % de instrucciones, líneas y ramas (`cd backend && mvn verify` → `target/site/jacoco/index.html`).
