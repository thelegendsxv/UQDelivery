import { useCallback, useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { actualizarTarifa, listarTarifas } from '../cotizacionApi.js'
import { aSolicitudTarifa, CAMPOS_TARIFA, validarTarifa } from '../cotizacionValidation.js'
import AppShell from '../components/AppShell.jsx'
import Field from '../components/Field.jsx'
import { formatearCOP } from '../components/QuoteResult.jsx'

const aFormulario = (t) => ({
    nombre: t.nombre ?? '',
    ...Object.fromEntries(Object.keys(CAMPOS_TARIFA).map((k) => [k, String(t[k] ?? '')])),
})

/** HU-05 - El administrador ve y edita la tarifa activa con la que se cotiza. */
export default function AdminTarifas() {
    const [tarifas, setTarifas] = useState([])
    const [values, setValues] = useState(null)
    const [errors, setErrors] = useState({})
    const [aviso, setAviso] = useState({ tipo: '', texto: '' })
    const [saving, setSaving] = useState(false)
    const [denegado, setDenegado] = useState(false)

    const activa = tarifas.find((t) => t.activa)

    const manejarError = useCallback((err, texto) => {
        if (err.status === 403) setDenegado(true)
        else setAviso({ tipo: 'error', texto: err.message || texto })
    }, [])

    useEffect(() => {
        listarTarifas()
            .then((lista) => {
                setTarifas(lista)
                const t = lista.find((x) => x.activa)
                if (t) setValues(aFormulario(t))
            })
            .catch((err) => manejarError(err, 'No se pudieron cargar las tarifas.'))
    }, [manejarError])

    const onChange = (e) => setValues({ ...values, [e.target.name]: e.target.value })

    async function onSubmit(e) {
        e.preventDefault()
        const found = validarTarifa(values)
        setErrors(found)
        if (Object.keys(found).length) return
        setSaving(true)
        try {
            const t = await actualizarTarifa(activa.id, aSolicitudTarifa(values))
            setTarifas((lista) => lista.map((x) => (x.id === t.id ? t : x)))
            setValues(aFormulario(t))
            setAviso({ tipo: 'ok', texto: 'La tarifa se guardó. Las nuevas cotizaciones ya usan estos valores.' })
        } catch (err) {
            setErrors(err.fieldErrors || {})
            manejarError(err, 'No se pudo guardar la tarifa.')
        } finally {
            setSaving(false)
        }
    }

    if (denegado) return <Navigate to="/acceso-denegado" replace />

    return (
        <AppShell narrow>
            <div className="page-head"><h1>Tarifas</h1></div>
            <p className="lede">Valores en pesos colombianos con los que se calcula la cotización de cada envío.</p>

            {aviso.texto && (
                <p className={`banner ${aviso.tipo === 'ok' ? 'banner-ok' : 'banner-error'}`}
                   role={aviso.tipo === 'ok' ? 'status' : 'alert'}>
                    {aviso.texto}
                </p>
            )}

            {!values ? (
                !aviso.texto && <p className="lede">{tarifas.length ? 'No hay una tarifa activa.' : 'Cargando…'}</p>
            ) : (
                <section className="panel">
                    <div className="quote-head">
                        <h2>Tarifa activa</h2>
                        <span className="chip chip-on">Activa</span>
                    </div>
                    <form onSubmit={onSubmit} noValidate>
                        <Field label="Nombre" name="nombre" value={values.nombre} onChange={onChange} error={errors.nombre} />
                        <div className="grid-form">
                            {Object.entries(CAMPOS_TARIFA).map(([campo, etiqueta]) => (
                                <Field key={campo} label={etiqueta} name={campo} value={values[campo]}
                                       onChange={onChange} error={errors[campo]} />
                            ))}
                        </div>
                        <p className="quote-note">
                            Ejemplo con estos valores: 5 km y 2 kg ={' '}
                            {formatearCOP(ejemplo(values))} sin recargos. El seguro aún no se aplica a la cotización.
                        </p>
                        <div className="actions">
                            <button className="btn btn-inline" type="submit" disabled={saving}>
                                {saving ? <><span className="spinner" aria-hidden="true" />Guardando…</> : 'Guardar tarifa'}
                            </button>
                        </div>
                    </form>
                </section>
            )}
        </AppShell>
    )
}

/** Vista previa en el cliente; el calculo oficial lo hace siempre el servidor. */
function ejemplo(v) {
    const n = (x) => Number(String(x).replace(',', '.')) || 0
    return n(v.tarifaBase) + 5 * n(v.costoPorKm) + 2 * n(v.costoPorKg)
}
