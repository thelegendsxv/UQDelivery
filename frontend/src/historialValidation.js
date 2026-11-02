// HU-14 · Historial de envios: filtros, textos de estado y formato de fecha y precio (sin dependencias de React).

export const ESTADOS_ENVIO = [
    { value: 'PENDIENTE_ASIGNACION', label: 'Pendiente de asignación' },
    { value: 'ASIGNADO', label: 'Asignado' },
    { value: 'EN_RUTA', label: 'En ruta' },
    { value: 'ENTREGADO', label: 'Entregado' },
    { value: 'CANCELADO', label: 'Cancelado' },
    { value: 'CON_NOVEDAD', label: 'Con novedad' },
]

export const FILTROS_VACIOS = { estado: '', desde: '', hasta: '' }

const FECHA_RE = /^\d{4}-\d{2}-\d{2}$/

/** Texto legible del estado; si llega uno desconocido se muestra tal cual. */
export function etiquetaEstado(estado) {
    return ESTADOS_ENVIO.find((e) => e.value === estado)?.label ?? String(estado ?? '')
}

/** Clase CSS de la etiqueta de color de un estado (ver historial.css). */
export function claseEstado(estado) {
    return `estado estado-${String(estado ?? '').toLowerCase().replace(/_/g, '-')}`
}

/** Errores por campo de los filtros. Las fechas son opcionales; si hay ambas, la inicial no puede ser posterior. */
export function validarFiltros(f = {}) {
    const errores = {}
    if (f.desde && !FECHA_RE.test(f.desde)) errores.desde = 'Escribe una fecha válida'
    if (f.hasta && !FECHA_RE.test(f.hasta)) errores.hasta = 'Escribe una fecha válida'
    if (!errores.desde && !errores.hasta && f.desde && f.hasta && f.desde > f.hasta) {
        errores.hasta = 'La fecha final no puede ser anterior a la inicial'
    }
    return errores
}

/** true si hay al menos un filtro aplicado. */
export function hayFiltros(f = {}) {
    return Boolean(f.estado || f.desde || f.hasta)
}

/** Texto de consulta para /api/envios/mis-envios: solo incluye los filtros con valor ("" si no hay ninguno). */
export function aQuery(f = {}) {
    const params = new URLSearchParams()
    if (f.estado) params.set('estado', f.estado)
    if (f.desde) params.set('desde', f.desde)
    if (f.hasta) params.set('hasta', f.hasta)
    const texto = params.toString()
    return texto ? `?${texto}` : ''
}

/** "02/11/2026 9:55 p. m." en hora de Colombia, o "" si la fecha no es valida. */
export function formatearFecha(iso) {
    const fecha = new Date(iso)
    if (!iso || Number.isNaN(fecha.getTime())) return ''
    return fecha.toLocaleString('es-CO', {
        timeZone: 'America/Bogota', day: '2-digit', month: '2-digit', year: 'numeric', hour: 'numeric', minute: '2-digit',
    })
}

/** 9564 -> "$ 9.564" (pesos colombianos sin decimales). */
export function formatearPrecio(valor) {
    const numero = Number(valor)
    if (valor === null || valor === undefined || valor === '' || !Number.isFinite(numero)) return ''
    return `$ ${Math.round(numero).toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.')}`
}
