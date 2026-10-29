import { clearSession, getSession } from './auth.js'

async function request(path, { method = 'GET', body, params } = {}) {
    const session = getSession()
    const query = params
        ? '?' + new URLSearchParams(Object.entries(params).filter(([, v]) => v !== '' && v != null)).toString()
        : ''
    const res = await fetch(`/api/usuarios${path}${query}`, {
        method,
        headers: {
            ...(body ? { 'Content-Type': 'application/json' } : {}),
            ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
        },
        body: body ? JSON.stringify(body) : undefined,
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
        // 401: token vencido o cuenta desactivada por el administrador -> se cierra la sesion.
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

export const listarUsuarios = ({ rol, activo, q, page = 0, size = 10 } = {}) =>
    request('', { params: { rol, activo, q, page, size } })
export const crearUsuario = (datos) => request('', { method: 'POST', body: datos })
export const cambiarRol = (id, role) => request(`/${id}/rol`, { method: 'PUT', body: { role } })
export const cambiarEstado = (id, activo) => request(`/${id}/estado`, { method: 'PATCH', body: { activo } })
export const miPerfil = () => request('/me')
export const actualizarMiPerfil = (datos) => request('/me', { method: 'PUT', body: datos })