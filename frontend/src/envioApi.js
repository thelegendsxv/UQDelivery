import { clearSession, getSession } from './auth.js'

// HU-04 · Llamadas a /api/envios (mismo estilo que cotizacionApi.js y usuariosApi.js)
async function request(path, { method = 'GET', body } = {}) {
    const session = getSession()
    const res = await fetch(`/api/envios${path}`, {
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

export const crearEnvio = (datos) => request('', { method: 'POST', body: datos })
export const misEnvios = () => request('/mis-envios')
export const obtenerEnvio = (codigo) => request(`/${codigo}`)
