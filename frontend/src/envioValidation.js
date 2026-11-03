// HU-04 · Validacion en el cliente; el backend repite las mismas reglas (CrearEnvioRequest).
// Las reglas de peso, medidas y coordenadas ya las valida QuoteForm (HU-05); aqui solo se agregan
// las direcciones (paso 1) y los datos del receptor (paso 2).
const NAME_RE = /^[\p{L} .'-]{2,100}$/u
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PHONE_RE = /^[0-9+() -]{7,20}$/
const NO_HTML_RE = /^[^<>]*$/

export const DIRECCIONES_VACIAS = { origenDireccion: '', destinoDireccion: '' }

export const RECEPTOR_VACIO = { receptorNombre: '', receptorTelefono: '', receptorEmail: '', descripcion: '' }

function direccion(valor, nombre) {
    const texto = String(valor ?? '').trim()
    if (!texto) return `Escribe la dirección de ${nombre}.`
    if (texto.length > 255) return 'La dirección no puede superar 255 caracteres.'
    if (!NO_HTML_RE.test(texto)) return 'La dirección no puede tener los caracteres < o >.'
    return null
}

export function validarDirecciones(v = {}) {
    const errors = {
        origenDireccion: direccion(v.origenDireccion, 'origen'),
        destinoDireccion: direccion(v.destinoDireccion, 'destino'),
    }
    return Object.fromEntries(Object.entries(errors).filter(([, e]) => e))
}

export function validarReceptor(v = {}) {
    const errors = {}
    const nombre = String(v.receptorNombre ?? '').trim()
    if (!nombre) errors.receptorNombre = 'Escribe el nombre del receptor.'
    else if (!NAME_RE.test(nombre))
        errors.receptorNombre = 'Usa solo letras, espacios, punto, apóstrofe o guion (de 2 a 100 caracteres).'

    const telefono = String(v.receptorTelefono ?? '').trim()
    if (!telefono) errors.receptorTelefono = 'Escribe el teléfono del receptor.'
    else if (!PHONE_RE.test(telefono))
        errors.receptorTelefono = 'Usa de 7 a 20 caracteres: números, espacios, +, ( ) o guion.'

    const email = String(v.receptorEmail ?? '').trim()
    if (email && !EMAIL_RE.test(email)) errors.receptorEmail = 'Revisa el correo: debe tener la forma nombre@dominio.com.'

    const descripcion = String(v.descripcion ?? '')
    if (descripcion.length > 255) errors.descripcion = 'La descripción no puede superar 255 caracteres.'
    else if (!NO_HTML_RE.test(descripcion)) errors.descripcion = 'La descripción no puede tener los caracteres < o >.'

    return errors
}

/**
 * Convierte los datos de los tres pasos al cuerpo de POST /api/envios.
 * `paquete` ya trae los campos numericos listos (pesoKg, origenLat...): los pone ahi QuoteForm,
 * a traves de `onResult(cotizacion, datos)`, con la misma conversion que usa para cotizar.
 */
export function aSolicitudEnvio(paquete, receptor) {
    return {
        pesoKg: paquete.pesoKg,
        largoCm: paquete.largoCm,
        anchoCm: paquete.anchoCm,
        altoCm: paquete.altoCm,
        origenDireccion: String(paquete.origenDireccion).trim(),
        origenLat: paquete.origenLat,
        origenLng: paquete.origenLng,
        destinoDireccion: String(paquete.destinoDireccion).trim(),
        destinoLat: paquete.destinoLat,
        destinoLng: paquete.destinoLng,
        esFragil: Boolean(paquete.esFragil),
        esUrgente: Boolean(paquete.esUrgente),
        receptorNombre: String(receptor.receptorNombre).trim(),
        receptorTelefono: String(receptor.receptorTelefono).trim(),
        receptorEmail: receptor.receptorEmail?.trim() || null,
        descripcion: receptor.descripcion?.trim() || null,
    }
}
