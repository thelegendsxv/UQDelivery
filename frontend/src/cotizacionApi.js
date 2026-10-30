import { clearSession, getSession } from './auth.js'

// HU-05 · Llamadas a /api/cotizaciones y /api/tarifas (mismo estilo que usuariosApi.js)
async function request(path, { method = 'GET', body } = {}) {
    const session = getSession()
    const res = await fetch(`/api${path}`, {
        method,
        headers: {
            ...(body ? { 'Content-Type': 'application/json' } : {}),
            ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
        },
        body: body ? JSON.stringify(body) : undefined,
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
        // 401: token vencido o cuenta desactivada -> se cierra la sesion.
        if (res.status === 401) {
            clearSession()
            window.location.assign('/login')
        }
        const err = new Error(data.message || 'Ocurrio un error inesperado')
        err.status = res.status
        err.fieldErrors = data.fieldErrors || {}
        throw err
    }
    return data
}

export const cotizar = (datos) => request('/cotizaciones', { method: 'POST', body: datos })
export const listarTarifas = () => request('/tarifas')
export const actualizarTarifa = (id, datos) => request(`/tarifas/${id}`, { method: 'PUT', body: datos })
