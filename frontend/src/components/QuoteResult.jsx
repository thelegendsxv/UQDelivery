import '../cotizacion.css'

const COP = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 2 })
const NUM = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 2 })

export const formatearCOP = (valor) => COP.format(Number(valor ?? 0))
export const formatearNumero = (valor) => NUM.format(Number(valor ?? 0))

/**
 * HU-05 - Desglose de una cotizacion (respuesta de POST /api/cotizaciones).
 * Props: cotizacion (obligatoria), title (opcional, por defecto "Cotización").
 */
export default function QuoteResult({ cotizacion, title = 'Cotización' }) {
    if (!cotizacion) return null
    const c = cotizacion
    return (
        <section className="quote-result" aria-live="polite">
            <div className="quote-head">
                <h2>{title}</h2>
                {c.estimado && <span className="chip chip-soon">Valor estimado</span>}
            </div>

            <p className="quote-total">{formatearCOP(c.total)}</p>

            <dl className="breakdown">
                <div className="breakdown-row">
                    <dt>Tarifa base</dt>
                    <dd>{formatearCOP(c.tarifaBase)}</dd>
                </div>
                <div className="breakdown-row">
                    <dt>Distancia <small>{formatearNumero(c.distanciaKm)} km × {formatearCOP(c.costoPorKm)}</small></dt>
                    <dd>{formatearCOP(c.costoDistancia)}</dd>
                </div>
                <div className="breakdown-row">
                    <dt>
                        Peso facturable <small>{formatearNumero(c.pesoFacturableKg)} kg × {formatearCOP(c.costoPorKg)}</small>
                        <small>Real {formatearNumero(c.pesoRealKg)} kg · volumétrico {formatearNumero(c.pesoVolumetricoKg)} kg</small>
                    </dt>
                    <dd>{formatearCOP(c.costoPeso)}</dd>
                </div>
                {c.recargos?.map((r) => (
                    <div className="breakdown-row" key={r.concepto}>
                        <dt>Recargo {r.concepto === 'Fragil' ? 'frágil' : r.concepto.toLowerCase()}</dt>
                        <dd>{formatearCOP(r.valor)}</dd>
                    </div>
                ))}

                <div className="breakdown-row total">
                    <dt>Total</dt>
                    <dd>{formatearCOP(c.total)}</dd>
                </div>
            </dl>

            <p className="quote-note">
                Valor estimado con la tarifa «{c.tarifa}» y la distancia en línea recta. El valor final puede cambiar
                cuando se confirme la ruta.
            </p>
        </section>
    )
}
