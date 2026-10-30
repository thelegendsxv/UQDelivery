// HU-03 - Validacion en el cliente. El backend repite estas mismas reglas.
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PASSWORD_RE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,72}$/
// El token del enlace son 32 bytes en Base64 URL: 43 caracteres.
const TOKEN_RE = /^[A-Za-z0-9_-]{43}$/

export function validateOlvido({ email }) {
  const errors = {}
  if (!email?.trim()) errors.email = 'Escribe tu correo.'
  else if (!EMAIL_RE.test(email.trim())) errors.email = 'Revisa el correo: debe tener la forma nombre@dominio.com.'
  return errors
}

export function validateReseteo({ password, confirmPassword }) {
  const errors = {}
  if (!password) errors.password = 'Crea una contraseña nueva.'
  else if (!PASSWORD_RE.test(password))
    errors.password = 'Debe tener de 8 a 72 caracteres, con mayúscula, minúscula y número.'
  if (confirmPassword !== password) errors.confirmPassword = 'Las contraseñas no coinciden.'
  return errors
}

export function tokenValido(token) {
  return typeof token === 'string' && TOKEN_RE.test(token)
}

// El backend responde 400 con este mensaje cuando el enlace no existe, vencio o ya se uso.
export function esEnlaceInvalido(err) {
  return Boolean(err?.fieldErrors?.token) || /enlace/i.test(err?.message || '')
}
