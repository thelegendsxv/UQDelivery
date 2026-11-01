# Búsqueda de lugares y distancia por calles · HU-04 y HU-05

Reemplaza la versión anterior (Nominatim desde el navegador + distancia en línea recta).

## Qué cambia para el usuario
- Un solo cuadro por lugar, con **autocompletado** (busca cuando se deja de escribir; sin selector de ciudad ni botón *Buscar*).
- Cada opción muestra el **nombre** y debajo **barrio · ciudad**. Se quitó el ruido administrativo (comuna, perímetro, departamento, RAP).
- Ya no se piden ni se muestran latitud y longitud.
- El precio usa la **distancia por las calles** (ruta en carro), como Google Maps, y no la línea recta.

## Cómo funciona
```
Navegador ──GET /api/lugares?q=...──▶ Backend (Spring) ──▶ TomTom Search API
Cotizar / Enviar ──▶ CotizacionService ──▶ RutaDistanciaService ──▶ TomTom Routing API
                                           └─ si falla o no hay clave: HaversineDistanciaService (línea recta)
```
La clave de TomTom vive **solo en el backend** (variable `TOMTOM_API_KEY`); el navegador nunca la ve.

## Por qué TomTom
Gratis y **sin tarjeta**, y una sola clave da búsqueda con autocompletado **y** rutas por calles.

| Opción | Gratis | Tarjeta | Búsqueda + rutas | Nota |
|---|---|---|---|---|
| **TomTom** (elegida) | 20.000 búsquedas y 20.000 rutas / mes (según su página de precios) | No | Sí | Datos propios, no solo OpenStreetMap |
| Google Maps | 10.000 por servicio / mes | **Sí** | Sí | Mejor dato en Colombia; exige facturación activa |
| Geoapify | 3.000 créditos / día | No | Sí | Basada en OpenStreetMap (mismo problema de números de casa) |
| Nominatim (la anterior) | Sin clave | No | Solo búsqueda | No permite autocompletar; sin ruta |

El proveedor está aislado en `TomTomMapasClient`: cambiar a Google u otro es reemplazar esa clase.

## Puesta en marcha
1. Crear cuenta en <https://developer.tomtom.com> y copiar la *API key*.
2. Definir la variable de entorno antes de arrancar el backend:
   - PowerShell: `$env:TOMTOM_API_KEY="tu-clave"` y luego `mvn spring-boot:run`
   - IntelliJ / VS Code: *Run configuration → Environment variables*
   - Docker: línea `TOMTOM_API_KEY=...` en `.env`
3. Probar: en *Cotizar*, escribir `Universidad del Quindío` y `Granada` y elegir; el desglose debe mostrar los km por calles.

Sin clave: `/api/lugares` responde 503 con mensaje claro y la cotización sigue funcionando con línea recta.

## Configuración (`application.yml`)
| Propiedad | Valor | Uso |
|---|---|---|
| `app.maps.tomtom-key` | `${TOMTOM_API_KEY:}` | Clave |
| `app.maps.centro-lat/lng` | Armenia | Centro de la búsqueda |
| `app.maps.ciudad` | `Armenia` | Solo se muestran resultados de este municipio |
| `app.maps.radio-metros` | `10000` | Limita resultados a la zona urbana (10 km); `0` = solo prioridad, sin límite |

## Limitaciones conocidas
- Los números de casa (`Cra 14 # 5-15`) casi nunca están en ningún mapa gratuito de Colombia: se ubica la calle o el barrio. Para el precio basta; el repartidor lee la dirección exacta que escribió el cliente.
- Si en la prueba una dirección importante no aparece en TomTom, el siguiente paso es *marcar el punto en un mapa* (Sprint 2) o cambiar de proveedor.
- Si TomTom falla en la cotización, se usa línea recta (queda un aviso en el log del backend).

## Archivos
| Archivo | Qué hace |
|---|---|
| `backend/.../service/TomTomMapasClient.java` | Llama a TomTom (búsqueda y ruta); no registra la clave |
| `backend/.../service/LugarMapper.java` | Deja título corto y subtítulo barrio · ciudad |
| `backend/.../service/RutaDistanciaService.java` | `@Primary`: distancia por calles con respaldo en línea recta |
| `backend/.../controller/LugarController.java` | `GET /api/lugares?q=` (usuario autenticado) |
| `backend/.../exception/*` | `MapasNoDisponibleException` → 503 |
| `frontend/src/lugaresApi.js` | Normaliza el texto (`Cra`→`Carrera`, quita `#`) y llama al backend |
| `frontend/src/components/AddressSearch.jsx` | Autocompletado con teclado (↑ ↓ Enter Esc) |

Pruebas: `LugarMapperTest` (5), `RutaDistanciaServiceTest` (5), `LugarControllerTest` (4), `lugaresApi.test.js` (7).

## Recuperación de contraseña: por qué el enlace sale en la terminal
Si `MAIL_HOST` está vacío, el backend escribe el enlace en el log (modo desarrollo). Para que llegue a Gmail:
1. Activar la verificación en 2 pasos y crear una *contraseña de aplicación* en <https://myaccount.google.com/apppasswords>.
2. Definir `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_USERNAME`, `MAIL_PASSWORD` (la de aplicación, no la normal), `MAIL_FROM` (el mismo Gmail).
3. Reiniciar el backend. Las variables del archivo `.env` solo las lee Docker; con `mvn spring-boot:run` hay que definirlas en la terminal o en el IDE.

## Puntos definidos por el equipo (entradas de la Universidad del Quindío)
`PuntosConocidos.java` guarda lugares con coordenadas fijas. Si el usuario escribe "Universidad del Quindío", solo aparecen esas entradas (no se consulta a TomTom). Para agregar una: en Google Maps, clic derecho sobre el sitio → clic en los números (latitud, longitud) → copiarlos en la lista.
