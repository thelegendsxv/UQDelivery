// HU-05 · Validacion en el cliente; el backend repite las mismas reglas (CotizacionRequest y TarifaRequest).
// Las claves de los errores coinciden con los nombres que devuelve el servidor en fieldErrors.
const NO_HTML_RE = /^[^<>]*$/
const DOS_DECIMALES_RE = /^\d+(\.\d{1,2})?$/
const MAX_MEDIDA = 10000

export const CAMPOS_COTIZACION = {
    pesoKg: 'Peso',
    largoCm: 'Largo',
    anchoCm: 'Ancho',
    altoCm: 'Alto',
    origenLat: 'Latitud de origen',
    origenLng: 'Longitud de origen',
    destinoLat: 'Latitud de destino',
    destinoLng: 'Longitud de destino',
}

export const COTIZACION_VACIA = {
    pesoKg: '', largoCm: '', anchoCm: '', altoCm: '',
    origenLat: '', origenLng: '', destinoLat: '', destinoLng: '',
    esFragil: false, esUrgente: false,
}

/** Acepta coma o punto decimal. Devuelve null si esta vacio y NaN si no es un numero. */
export function aNumero(valor) {
    const texto = String(valor ?? '').trim().replace(',', '.')
    if (!texto) return null
    return /^-?\d+(\.\d+)?$/.test(texto) ? Number(texto) : NaN
}

function medida(valor, nombre, unidad) {
    const n = aNumero(valor)
    if (n === null) return `Escribe el ${nombre}.`
    if (Number.isNaN(n)) return `El ${nombre} debe ser un número.`
    if (n <= 0) return `El ${nombre} debe ser mayor que 0.`
    if (n > MAX_MEDIDA) return `El ${nombre} no puede superar ${MAX_MEDIDA} ${unidad}.`
    return null
}

function coordenada(valor, nombre, limite) {
    const n = aNumero(valor)
    if (n === null) return `Escribe la ${nombre}.`
    if (Number.isNaN(n)) return `La ${nombre} debe ser un número.`
    if (n < -limite || n > limite) return `La ${nombre} debe estar entre -${limite} y ${limite}.`
    return null
}

export function validarCotizacion(v = {}) {
    const errors = {
        pesoKg: medida(v.pesoKg, 'peso', 'kg'),
        largoCm: medida(v.largoCm, 'largo', 'cm'),
        anchoCm: medida(v.anchoCm, 'ancho', 'cm'),
        altoCm: medida(v.altoCm, 'alto', 'cm'),
        origenLat: coordenada(v.origenLat, 'latitud de origen', 90),
        origenLng: coordenada(v.origenLng, 'longitud de origen', 180),
        destinoLat: coordenada(v.destinoLat, 'latitud de destino', 90),
        destinoLng: coordenada(v.destinoLng, 'longitud de destino', 180),
    }
    return Object.fromEntries(Object.entries(errors).filter(([, e]) => e))
}

/** Convierte los valores del formulario al cuerpo de POST /api/cotizaciones. */
export function aSolicitudCotizacion(v) {
    const datos = Object.fromEntries(Object.keys(CAMPOS_COTIZACION).map((k) => [k, aNumero(v[k])]))
    return { ...datos, esFragil: Boolean(v.esFragil), esUrgente: Boolean(v.esUrgente) }
}

export const CAMPOS_TARIFA = {
    tarifaBase: 'Tarifa base',
    costoPorKm: 'Costo por km',
    costoPorKg: 'Costo por kg',
    recargoFragil: 'Recargo por frágil',
    recargoUrgente: 'Recargo por urgente',
    porcentajeSeguro: 'Seguro (%)',
}

export function validarTarifa(v = {}) {
    const errors = {}
    const nombre = String(v.nombre ?? '').trim()
    if (!nombre) errors.nombre = 'Escribe el nombre de la tarifa.'
    else if (nombre.length > 60) errors.nombre = 'El nombre no puede superar 60 caracteres.'
    else if (!NO_HTML_RE.test(nombre)) errors.nombre = 'El nombre no puede tener los caracteres < o >.'
    for (const [campo, etiqueta] of Object.entries(CAMPOS_TARIFA)) {
        const texto = String(v[campo] ?? '').trim().replace(',', '.')
        if (!texto) errors[campo] = `Escribe ${etiqueta.toLowerCase()}.`
        else if (!DOS_DECIMALES_RE.test(texto)) errors[campo] = 'Usa un valor no negativo con máximo 2 decimales.'
        else if (campo === 'porcentajeSeguro' && Number(texto) > 100) errors[campo] = 'El porcentaje no puede superar 100.'
    }
    return errors
}

/** Convierte los valores del formulario al cuerpo de PUT /api/tarifas/{id}. */
export function aSolicitudTarifa(v) {
    const datos = Object.fromEntries(Object.keys(CAMPOS_TARIFA).map((k) => [k, aNumero(v[k])]))
    return { nombre: String(v.nombre).trim(), ...datos }
}
