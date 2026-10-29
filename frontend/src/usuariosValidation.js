// HU-02 · Validacion en el cliente; el backend repite las mismas reglas.
const NAME_RE = /^[\p{L} .'-]{2,100}$/u
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PASSWORD_RE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,72}$/
const PHONE_RE = /^[0-9+() -]{7,20}$/
const NO_HTML_RE = /^[^<>]*$/
const ROLES_VALIDOS = ['ADMINISTRADOR', 'CLIENTE_EMISOR', 'REPARTIDOR']

export function validarPerfil({ nombre = '', telefono = '', direccion = '' }) {
    const errors = {}
    if (!nombre.trim()) errors.nombre = 'Escribe tu nombre.'
    else if (!NAME_RE.test(nombre.trim()))
        errors.nombre = 'Usa solo letras, espacios, punto, apóstrofe o guion (de 2 a 100 caracteres).'
    if (telefono.trim() && !PHONE_RE.test(telefono.trim()))
        errors.telefono = 'Usa de 7 a 20 caracteres: números, espacios, +, ( ) o guion.'
    if (direccion.length > 200) errors.direccion = 'La dirección no puede superar 200 caracteres.'
    else if (!NO_HTML_RE.test(direccion)) errors.direccion = 'La dirección no puede tener los caracteres < o >.'
    return errors
}

export function validarNuevoUsuario({ nombre = '', email = '', password = '', role = '' }) {
    const errors = {}
    if (!nombre.trim()) errors.nombre = 'Escribe el nombre.'
    else if (!NAME_RE.test(nombre.trim()))
        errors.nombre = 'Usa solo letras, espacios, punto, apóstrofe o guion (de 2 a 100 caracteres).'
    if (!email.trim()) errors.email = 'Escribe el correo.'
    else if (!EMAIL_RE.test(email.trim()) || email.length > 150)
        errors.email = 'Revisa el correo: debe tener la forma nombre@dominio.com.'
    if (!PASSWORD_RE.test(password))
        errors.password = 'Debe tener de 8 a 72 caracteres, con mayúscula, minúscula y número.'
    if (!ROLES_VALIDOS.includes(role)) errors.role = 'Elige un rol.'
    return errors
}