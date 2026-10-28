// Validación en el cliente. El backend repite estas mismas reglas (nunca confiar solo en el cliente).
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const NAME_RE = /^[\p{L} .'-]{2,100}$/u
const PASSWORD_RE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,72}$/

export function validateRegister({ nombre, email, password, confirmPassword }) {
  const errors = {}
  if (!nombre?.trim()) errors.nombre = 'Escribe tu nombre.'
  else if (!NAME_RE.test(nombre.trim()))
    errors.nombre = 'Usa solo letras, espacios, punto, apóstrofe o guion (de 2 a 100 caracteres).'

  if (!email?.trim()) errors.email = 'Escribe tu correo.'
  else if (!EMAIL_RE.test(email.trim())) errors.email = 'Revisa el correo: debe tener la forma nombre@dominio.com.'

  if (!password) errors.password = 'Crea una contraseña.'
  else if (!PASSWORD_RE.test(password))
    errors.password = 'Debe tener de 8 a 72 caracteres, con mayúscula, minúscula y número.'

  if (confirmPassword !== password) errors.confirmPassword = 'Las contraseñas no coinciden.'
  return errors
}

export function validateLogin({ email, password }) {
  const errors = {}
  if (!email?.trim()) errors.email = 'Escribe tu correo.'
  else if (!EMAIL_RE.test(email.trim())) errors.email = 'Revisa el correo: debe tener la forma nombre@dominio.com.'
  if (!password) errors.password = 'Escribe tu contraseña.'
  return errors
}
