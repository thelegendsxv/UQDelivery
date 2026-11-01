import { clearSession, getSession } from './auth.js'

// Busqueda de lugares: el navegador le pregunta a NUESTRO backend (GET /api/lugares?q=...) y el backend
// consulta a TomTom con la clave guardada en el servidor. Asi la clave nunca llega al navegador.
export const MIN_CARACTERES = 3

const ABREVIATURAS = [
    [/\b(cra|cr|kr|kra)\.?(?=\s*\d)/gi, 'Carrera'],
    [/\b(cl|cll)\.?(?=\s*\d)/gi, 'Calle'],
    [/\bav\.?(?=\s)/gi, 'Avenida'],
    [/\bdg\.?(?=\s*\d)/gi, 'Diagonal'],
    [/\b(tv|trans)\.?(?=\s*\d)/gi, 'Transversal'],
]

/** "Cra 14 # 5-15 barrio Granada" -> "Carrera 14 5-15 barrio Granada" (sin # ni abreviaturas). */
export function normalizarConsulta(texto) {
    return ABREVIATURAS
        .reduce((t, [patron, reemplazo]) => t.replace(patron, reemplazo), String(texto ?? ''))
        .replace(/\s*(?:#|\bn[o°º]\.)\s*/gi, ' ')
        .replace(/\s+/g, ' ')
        .trim()
}

/**
 * Devuelve [{ id, titulo, subtitulo, lat, lng }]. Lanza Error si el servicio falla.
 * `fetchImpl` se puede reemplazar en las pruebas.
 */
export async function buscarLugares(texto, { signal, fetchImpl = globalThis.fetch } = {}) {
    const q = normalizarConsulta(texto)
    if (q.length < MIN_CARACTERES) return []
    const session = getSession()
    const res = await fetchImpl(`/api/lugares?${new URLSearchParams({ q })}`, {
        headers: { Accept: 'application/json', ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}) },
        signal,
    })
    const data = await res.json().catch(() => (res.ok ? [] : {}))
    if (!res.ok) {
        if (res.status === 401) {
            clearSession()
            window.location.assign('/login')
        }
        const err = new Error(data.message || 'No pudimos consultar el mapa.')
        err.status = res.status
        throw err
    }
    return Array.isArray(data) ? data : []
}
