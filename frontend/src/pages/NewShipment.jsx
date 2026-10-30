import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import AppShell from '../components/AppShell.jsx'
import Field from '../components/Field.jsx'
import QuoteForm from '../components/QuoteForm.jsx'
import QuoteResult from '../components/QuoteResult.jsx'
import { crearEnvio } from '../envioApi.js'
import { COTIZACION_VACIA } from '../cotizacionValidation.js'
import { DIRECCIONES_VACIAS, RECEPTOR_VACIO, aSolicitudEnvio, validarDirecciones, validarReceptor } from '../envioValidation.js'
import '../cotizacion.css'
import '../envio.css'

const PASOS = ['Paquete y ubicación', 'Receptor', 'Resumen']

/**
 * HU-04 - El cliente emisor crea una solicitud de envio en pasos. El paso 1 reutiliza QuoteForm
 * (HU-05) para el peso, las medidas y las coordenadas, y de paso calcula el precio estimado: el
 * precio y la distancia que se guardan los recalcula el servidor con los mismos datos (nunca los
 * decide el cliente). Las coordenadas se escriben a mano; el mapa llega en el Sprint 2 (igual que
 * en la pagina de cotizacion).
 */
export default function NewShipment() {
    const navigate = useNavigate()
    const [paso, setPaso] = useState(0)
    const [paquete, setPaquete] = useState({ ...COTIZACION_VACIA, ...DIRECCIONES_VACIAS })
    const [paqueteErrors, setPaqueteErrors] = useState({})
    const [cotizacion, setCotizacion] = useState(null)
    const [receptor, setReceptor] = useState(RECEPTOR_VACIO)
    const [receptorErrors, setReceptorErrors] = useState({})
    const [envio, setEnvio] = useState(null)
    const [aviso, setAviso] = useState('')
    const [enviando, setEnviando] = useState(false)
    const [denegado, setDenegado] = useState(false)

    if (denegado) return <Navigate to="/acceso-denegado" replace />

    function onDireccion(e) {
        setPaquete({ ...paquete, [e.target.name]: e.target.value })
        setCotizacion(null) // si cambia la direccion, conviene volver a cotizar
    }

    function onCotizacionLista(c, datos) {
        setCotizacion(c)
        setPaquete((p) => ({ ...p, ...datos }))
        setAviso('')
    }

    function irAReceptor() {
        const found = validarDirecciones(paquete)
        setPaqueteErrors(found)
        if (Object.keys(found).length) {
            setAviso('Revisa las direcciones de origen y destino.')
            return
        }
        if (!cotizacion) {
            setAviso('Calcula el precio antes de continuar.')
            return
        }
        setAviso('')
        setPaso(1)
    }

    function irAResumen(e) {
        e.preventDefault()
        const found = validarReceptor(receptor)
        setReceptorErrors(found)
        if (Object.keys(found).length) return
        setAviso('')
        setPaso(2)
    }

    async function confirmar() {
        setEnviando(true)
        setAviso('')
        try {
            const creado = await crearEnvio(aSolicitudEnvio(paquete, receptor))
            setEnvio(creado)
            setPaso(3)
        } catch (err) {
            if (err.status === 403) { setDenegado(true); return }
            setAviso(err.message || 'No se pudo crear el envío.')
        } finally {
            setEnviando(false)
        }
    }

    async function copiarCodigo() {
        try {
            await navigator.clipboard.writeText(envio.codigoSeguimiento)
            setAviso('Código copiado.')
        } catch {
            setAviso('No se pudo copiar automáticamente: copia el código a mano.')
        }
    }

    const onReceptorInput = (e) => setReceptor({ ...receptor, [e.target.name]: e.target.value })

    return (
        <AppShell narrow={paso === 3}>
            <div className="page-head"><h1>Solicitar un envío</h1></div>

            {paso < 3 && (
                <ol className="steps">
                    {PASOS.map((titulo, i) => (
                        <li key={titulo} className={i === paso ? 'is-current' : i < paso ? 'is-done' : ''}>{titulo}</li>
                    ))}
                </ol>
            )}

            {aviso && <p className="banner banner-error" role="alert">{aviso}</p>}

            {paso === 0 && (
                <div className="quote-layout">
                    <section className="panel">
                        <h2>Paquete y ubicación</h2>
                        <div className="grid-form">
                            <Field label="Dirección de origen" name="origenDireccion" value={paquete.origenDireccion}
                                   onChange={onDireccion} error={paqueteErrors.origenDireccion} />
                            <Field label="Dirección de destino" name="destinoDireccion" value={paquete.destinoDireccion}
                                   onChange={onDireccion} error={paqueteErrors.destinoDireccion} />
                        </div>
                        <p className="lede">Agrega también las coordenadas de origen y destino (el selector con mapa llega en el Sprint 2).</p>
                        <QuoteForm embedded submitLabel="Calcular precio" initialValues={paquete}
                                   onResult={onCotizacionLista} onError={() => setCotizacion(null)} />
                        <div className="actions">
                            <button className="btn btn-inline" type="button" onClick={irAReceptor}>Continuar</button>
                        </div>
                    </section>
                    <section className="panel">
                        {cotizacion
                            ? <QuoteResult cotizacion={cotizacion} title="Precio estimado" />
                            : <p className="quote-empty">Calcula el precio para continuar.</p>}
                    </section>
                </div>
            )}

            {paso === 1 && (
                <section className="panel">
                    <h2>Datos del receptor</h2>
                    <form className="grid-form" onSubmit={irAResumen} noValidate>
                        <Field label="Nombre del receptor" name="receptorNombre" value={receptor.receptorNombre}
                               onChange={onReceptorInput} error={receptorErrors.receptorNombre} />
                        <Field label="Teléfono del receptor" name="receptorTelefono" value={receptor.receptorTelefono}
                               onChange={onReceptorInput} error={receptorErrors.receptorTelefono} />
                        <Field label="Correo del receptor (opcional)" name="receptorEmail" type="email"
                               value={receptor.receptorEmail} onChange={onReceptorInput} error={receptorErrors.receptorEmail} />
                        <Field label="Contenido del paquete (opcional)" name="descripcion" value={receptor.descripcion}
                               onChange={onReceptorInput} error={receptorErrors.descripcion} />
                        <div className="actions">
                            <button className="btn-ghost" type="button" onClick={() => setPaso(0)}>Atrás</button>
                            <button className="btn btn-inline" type="submit">Continuar</button>
                        </div>
                    </form>
                </section>
            )}

            {paso === 2 && (
                <div className="quote-layout">
                    <section className="panel">
                        <h2>Resumen</h2>
                        <dl className="breakdown">
                            <div className="breakdown-row"><dt>Origen</dt><dd>{paquete.origenDireccion}</dd></div>
                            <div className="breakdown-row"><dt>Destino</dt><dd>{paquete.destinoDireccion}</dd></div>
                            <div className="breakdown-row"><dt>Receptor</dt><dd>{receptor.receptorNombre} · {receptor.receptorTelefono}</dd></div>
                            {receptor.descripcion && <div className="breakdown-row"><dt>Contenido</dt><dd>{receptor.descripcion}</dd></div>}
                        </dl>
                        <div className="actions">
                            <button className="btn-ghost" type="button" onClick={() => setPaso(1)} disabled={enviando}>Atrás</button>
                            <button className="btn btn-inline" type="button" onClick={confirmar} disabled={enviando}>
                                {enviando ? <><span className="spinner" aria-hidden="true" />Enviando…</> : 'Confirmar solicitud'}
                            </button>
                        </div>
                    </section>
                    <section className="panel">
                        <QuoteResult cotizacion={cotizacion} title="Precio estimado" />
                    </section>
                </div>
            )}

            {paso === 3 && envio && (
                <section className="panel confirmacion">
                    <h2>¡Envío creado!</h2>
                    <p className="lede">Guarda este código para hacer seguimiento a tu paquete.</p>
                    <p className="codigo-seguimiento">{envio.codigoSeguimiento}</p>
                    <div className="actions">
                        <button className="btn-ghost" type="button" onClick={copiarCodigo}>Copiar código</button>
                        <button className="btn btn-inline" type="button" onClick={() => navigate('/')}>Volver al inicio</button>
                    </div>
                </section>
            )}
        </AppShell>
    )
}
