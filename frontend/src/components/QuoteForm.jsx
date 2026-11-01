import { useEffect, useState } from 'react'
import { cotizar } from '../cotizacionApi.js'
import {
    aSolicitudCotizacion, CAMPOS_COTIZACION, COTIZACION_VACIA, validarCotizacion,
} from '../cotizacionValidation.js'
import AddressSearch from './AddressSearch.jsx'
import Field from './Field.jsx'
import '../cotizacion.css'

/**
 * HU-05 - Formulario de cotizacion reutilizable (pagina Quote y formulario de envio de HU-04).
 *
 * Props (todas opcionales):
 *  - initialValues: valores iniciales, p. ej. { pesoKg: 2, origenLat: 4.53, esFragil: true }. Si cambian, se aplican.
 *    Si trae origenDireccion / destinoDireccion, el cuadro de busqueda de cada lugar muestra ese texto.
 *  - onResult(cotizacion, datos): se llama con la respuesta del servidor y el cuerpo enviado.
 *  - onError(err): se llama si el servidor responde con error (err.status, err.message, err.fieldErrors).
 *  - onChange(values): se llama cada vez que cambia un campo (valores tal como se escriben).
 *  - onLugar(prefijo, lugar): se llama al elegir (o quitar) el origen o el destino en el buscador;
 *    prefijo es 'origen' o 'destino' y lugar { etiqueta, lat, lng } o null.
 *  - onQuote(datos): reemplaza la llamada por defecto a POST /api/cotizaciones; debe devolver una promesa.
 *  - embedded: true para usarlo dentro de otro <form> (se dibuja como <div> y el boton no envia el formulario padre).
 *  - submitLabel: texto del boton (por defecto "Cotizar").
 */
export default function QuoteForm({
    initialValues, onResult, onError, onChange, onLugar, onQuote, embedded = false, submitLabel = 'Cotizar',
}) {
    const [values, setValues] = useState(() => ({ ...COTIZACION_VACIA, ...limpiar(initialValues) }))
    const [errors, setErrors] = useState({})
    const [aviso, setAviso] = useState('')
    const [loading, setLoading] = useState(false)

    const inicialKey = JSON.stringify(initialValues ?? {})
    useEffect(() => {
        // Se compara por contenido (inicialKey) para no reiniciar el formulario en cada render del padre.
        if (initialValues) setValues((v) => ({ ...v, ...limpiar(initialValues) }))
    }, [inicialKey])

    function actualizar(nuevos) {
        setValues(nuevos)
        onChange?.(nuevos)
    }

    const onInput = (e) => actualizar({ ...values, [e.target.name]: e.target.value })
    const onCheck = (e) => actualizar({ ...values, [e.target.name]: e.target.checked })

    // El buscador de direcciones llena (o vacia) la latitud y la longitud del origen o del destino.
    function fijarLugar(prefijo, lugar) {
        actualizar({
            ...values,
            [`${prefijo}Lat`]: lugar ? String(lugar.lat) : '',
            [`${prefijo}Lng`]: lugar ? String(lugar.lng) : '',
        })
        onLugar?.(prefijo, lugar)
    }

    async function enviar(e) {
        e?.preventDefault()
        const found = validarCotizacion(values)
        setErrors(found)
        setAviso(Object.keys(found).length ? pendientes(found) : '')
        if (Object.keys(found).length) return
        const datos = aSolicitudCotizacion(values)
        setLoading(true)
        try {
            const cotizacion = await (onQuote ? onQuote(datos) : cotizar(datos))
            onResult?.(cotizacion, datos)
        } catch (err) {
            // Criterio 2: el servidor devuelve 400 con los datos pendientes en fieldErrors y sin precio.
            const campos = err.fieldErrors || {}
            setErrors(campos)
            setAviso(Object.keys(campos).length ? pendientes(campos) : err.message || 'No se pudo cotizar el envío.')
            onError?.(err)
        } finally {
            setLoading(false)
        }
    }

    const Contenedor = embedded ? 'div' : 'form'
    const contenedorProps = embedded ? {} : { onSubmit: enviar, noValidate: true }

    return (
        <Contenedor className="quote-form" {...contenedorProps}>
            {aviso && <p className="banner banner-error" role="alert">{aviso}</p>}

            <fieldset className="quote-group">
                <legend>Paquete</legend>
                <div className="grid-form">
                    <Field label="Peso (kg)" name="pesoKg" value={values.pesoKg} onChange={onInput} error={errors.pesoKg} />
                    <Field label="Largo (cm)" name="largoCm" value={values.largoCm} onChange={onInput} error={errors.largoCm} />
                    <Field label="Ancho (cm)" name="anchoCm" value={values.anchoCm} onChange={onInput} error={errors.anchoCm} />
                    <Field label="Alto (cm)" name="altoCm" value={values.altoCm} onChange={onInput} error={errors.altoCm} />
                </div>
            </fieldset>

            <fieldset className="quote-group">
                <legend>Origen y destino</legend>
                <AddressSearch etiqueta="Origen" name="origen" textoInicial={initialValues?.origenDireccion} lat={values.origenLat} lng={values.origenLng}
                               error={errors.origenLat || errors.origenLng} onSelect={(l) => fijarLugar('origen', l)} />
                <AddressSearch etiqueta="Destino" name="destino" textoInicial={initialValues?.destinoDireccion} lat={values.destinoLat} lng={values.destinoLng}
                               error={errors.destinoLat || errors.destinoLng} onSelect={(l) => fijarLugar('destino', l)} />
                <p className="geo-attrib">Ubicaciones con datos de TomTom.</p>
            </fieldset>

            <div className="checks">
                <label className="check">
                    <input type="checkbox" name="esFragil" checked={values.esFragil} onChange={onCheck} />
                    Es frágil
                </label>
                <label className="check">
                    <input type="checkbox" name="esUrgente" checked={values.esUrgente} onChange={onCheck} />
                    Es urgente
                </label>
            </div>

            <div className="actions">
                <button className="btn btn-inline" type={embedded ? 'button' : 'submit'} disabled={loading}
                        onClick={embedded ? enviar : undefined}>
                    {loading ? <><span className="spinner" aria-hidden="true" />Cotizando…</> : submitLabel}
                </button>
            </div>
        </Contenedor>
    )
}

/** Pasa los valores iniciales a texto para los campos (las casillas quedan como booleanos). */
function limpiar(iniciales = {}) {
    return Object.fromEntries(Object.entries(iniciales ?? {})
        .filter(([k]) => k in COTIZACION_VACIA)
        .map(([k, v]) => [k, typeof COTIZACION_VACIA[k] === 'boolean' ? Boolean(v) : String(v ?? '')]))
}

function pendientes(errores) {
    // El servidor no garantiza el orden de fieldErrors: se muestran en el orden del formulario.
    const orden = Object.keys(CAMPOS_COTIZACION)
    const nombres = Object.keys(errores)
        .sort((a, b) => orden.indexOf(a) - orden.indexOf(b))
        .map((k) => CAMPOS_COTIZACION[k] ?? k)
    return `Revisa estos datos antes de cotizar: ${nombres.join(', ')}.`
}
