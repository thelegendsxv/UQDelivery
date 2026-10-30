// HU-03 - Recuperacion de contrasena. Endpoints publicos: no se envia token de sesion.
async function request(path, body) {
  const res = await fetch(`/api/auth${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  const data = await res.json().catch(() => ({}))
  if (!res.ok) {
    const err = new Error(data.message || 'Ocurrio un error inesperado')
    err.status = res.status
    err.fieldErrors = data.fieldErrors || {}
    throw err
  }
  return data
}

export const solicitarRecuperacion = (email) => request('/forgot-password', { email: email.trim() })
export const restablecerContrasena = ({ token, password }) => request('/reset-password', { token, password })
