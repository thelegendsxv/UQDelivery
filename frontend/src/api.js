async function request(path, body, token) {
  const res = await fetch(`/api/auth${path}`, {
    method: body ? 'POST' : 'GET',
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
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

export const register = ({ nombre, email, password }) =>
  request('/register', { nombre: nombre.trim(), email: email.trim(), password })
export const login = ({ email, password }) => request('/login', { email: email.trim(), password })
export const me = (token) => request('/me', null, token)
