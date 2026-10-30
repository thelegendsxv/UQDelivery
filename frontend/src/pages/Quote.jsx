import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import AppShell from '../components/AppShell.jsx'
import QuoteForm from '../components/QuoteForm.jsx'
import QuoteResult from '../components/QuoteResult.jsx'

/** HU-05 - El cliente emisor cotiza un envio antes de solicitarlo. */
export default function Quote() {
    const [cotizacion, setCotizacion] = useState(null)
    const [denegado, setDenegado] = useState(false)

    function onError(err) {
        setCotizacion(null)
        if (err.status === 403) setDenegado(true)
    }

    if (denegado) return <Navigate to="/acceso-denegado" replace />

    return (
        <AppShell>
            <div className="page-head"><h1>Cotizar envío</h1></div>
            <p className="lede">Escribe el peso, las medidas y las coordenadas de origen y destino para conocer el precio estimado.</p>

            <div className="quote-layout">
                <section className="panel">
                    <QuoteForm onResult={(c) => setCotizacion(c)} onError={onError} />
                </section>
                <section className="panel">
                    {cotizacion
                        ? <QuoteResult cotizacion={cotizacion} />
                        : <p className="quote-empty">Aquí verás el desglose del precio cuando cotices.</p>}
                </section>
            </div>
        </AppShell>
    )
}
