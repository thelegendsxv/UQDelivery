# HU-14 · Historial de envíos personalizado

Responsable: Jose Manuel Bedoya Mondragón · Rama: `feature/HU-14-historial-envios` · 3 puntos

> Como cliente emisor, quiero consultar el historial de mis envíos con sus estados y fechas, para revisar las
> operaciones realizadas y hacer seguimiento a mis pedidos anteriores.

## Cómo probarlo

1. Entrar como cliente emisor (registrarse en `/registro`).
2. Crear uno o más envíos en **Enviar paquete** (HU-04).
3. Abrir **Mis envíos** (menú superior, o *Consultar tu historial* en el inicio): `/envios`.
4. Filtrar por **estado** y/o por **fechas** (*Desde* y *Hasta*) y pulsar **Filtrar**; **Limpiar** vuelve a mostrar todo.
5. **Ver detalle** despliega los datos del envío (receptor, peso, medidas, distancia, frágil/urgente).

Un envío nuevo siempre queda en *Pendiente de asignación*. Los demás estados (asignado, en ruta, entregado, cancelado,
con novedad) los cambian historias posteriores (HU-06, 10, 11 y 13); hasta entonces el filtro funciona con los datos que haya.

## Reglas

- Cada cliente ve **solo sus envíos**: la consulta siempre se limita al correo del usuario autenticado, también cuando
  hay filtros. Un repartidor o un administrador recibe 403.
- Los filtros son opcionales y se combinan (estado **y** fechas). Sin filtros devuelve todo el historial.
- Las fechas son de **creación** del envío, **inclusivas** en ambos extremos y en **hora de Colombia**: `desde=hasta=2026-11-02`
  muestra todo lo creado ese día.
- Orden: del más reciente al más antiguo.
- Si la fecha inicial es posterior a la final, o el estado o la fecha no son válidos, responde **400** y no consulta nada.
  La pantalla también lo valida antes de enviar.

## API

| Método | Ruta | Rol | Respuestas |
|---|---|---|---|
| GET | `/api/envios/mis-envios?estado=&desde=&hasta=` | CLIENTE_EMISOR | 200 (lista) · 400 (filtro inválido) · 401 · 403 |

- `estado`: `PENDIENTE_ASIGNACION`, `ASIGNADO`, `EN_RUTA`, `ENTREGADO`, `CANCELADO` o `CON_NOVEDAD`.
- `desde` y `hasta`: `AAAA-MM-DD`.
- Es el mismo endpoint de HU-04; sin parámetros se comporta exactamente igual que antes.

## Criterios de aceptación → implementación

| Criterio | Dónde |
|---|---|
| 1. El cliente con envíos ve **únicamente los asociados a su cuenta** | `EnvioService.misEnvios`: el correo del token es siempre la primera condición. Prueba: `losFiltrosNuncaMuestranEnviosDeOtroCliente` |
| 2. Con un filtro por estado o fechas, ve **solo los que cumplen** | `Specification` con las condiciones que se hayan enviado. Pruebas: `filtrarPorEstadoMuestraSoloLosEnviosEnEseEstado`, `filtrarPorFechasIncluyeElDiaDeCreacionYExcluyeLosDemasDias` |

## Archivos nuevos

| Archivo | Qué hace |
|---|---|
| `exception/FiltroInvalidoException.java` | Rango de fechas incoherente → 400 |
| `frontend/src/pages/MyShipments.jsx` · `historial.css` | Pantalla *Mis envíos*: filtros, tabla, estados de color y detalle desplegable |
| `frontend/src/historialValidation.js` (+ test) | Validación de filtros, texto de la consulta, etiquetas de estado y formato de fecha y precio |

## Cambios sobre archivos de otros

| Archivo | Cambio | Motivo |
|---|---|---|
| `controller/EnvioController.java` | `mis-envios` acepta `estado`, `desde` y `hasta` (opcionales) | Filtros del historial |
| `service/EnvioService.java` | Nuevo `misEnvios(email, estado, desde, hasta)`; el de un solo parámetro se conserva | Consulta con filtros |
| `repository/EnvioRepository.java` | Además es `JpaSpecificationExecutor` | Armar la consulta según los filtros |
| `exception/GlobalExceptionHandler.java` | + handler de `FiltroInvalidoException` → 400 | Mensaje claro |
| `frontend/src/envioApi.js` | `misEnvios(filtros)` | Enviar los filtros |
| `frontend/src/App.jsx` · `components/AppShell.jsx` · `pages/Home.jsx` | Ruta `/envios`, enlace *Mis envíos* y acceso desde *Consultar tu historial* | Llegar a la pantalla |

## Pruebas

- `EnvioServiceTest` (+4): sin filtros usa la consulta simple, con filtros usa la especificación, rango invertido rechazado
  sin consultar, y un mismo día como inicio y fin es válido.
- `EnvioControllerTest` (+6): filtro por estado, por fechas, aislamiento entre clientes, rango invertido (400),
  estado o fecha inválidos (400) y repartidor sin acceso (403).
- `historialValidation.test.js` (Vitest, 12).

## Supuestos

- **Sin paginación:** devuelve todos los envíos del cliente, como el endpoint de HU-04. Si un cliente llegara a tener
  muchos, se pagina igual que en HU-02.
- **Se filtra por fecha de creación:** no hay aún otra fecha por estado; cuando HU-06, 10, 11 y 13 registren los cambios
  de estado, el historial de estados (`historial_estado_envio`) ya tiene lo necesario para mostrar la línea de tiempo.
