import { Fragment, useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import AppShell from '../components/AppShell.jsx'
import Field from '../components/Field.jsx'
import SelectField from '../components/SelectField.jsx'
import { misEnvios } from '../envioApi.js'
import {
    claseEstado, ESTADOS_ENVIO, etiquetaEstado, FILTROS_VACIOS, formatearFecha, formatearPrecio, hayFiltros, validarFiltros,
} from '../historialValidation.js'
import '../historial.css'

const OPCIONES_ESTADO = [{ value: '', label: 'Todos los estados' }, ...ESTADOS_ENVIO]

function Dato({ etiqueta, valor }) {
    if (valor === null || valor === undefined || valor === '') return null
    return <div className="dato"><dt>{etiqueta}</dt><dd>{valor}</dd></div>
}

/** HU-14 - Historial de envios del cliente emisor, con filtros por estado y fechas. */
export default function MyShipments() {
    const [filtros, setFiltros] = useState(FILTROS_VACIOS) // lo que se escribe
    const [aplicados, setAplicados] = useState(FILTROS_VACIOS) // lo que se busco con "Filtrar"
    const [errores, setErrores] = useState({})
    const [envios, setEnvios] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [abierto, setAbierto] = useState(null) // codigo del envio con el detalle desplegado

    const cargar = useCallback(async (f) => {
        setLoading(true)
        setError('')
        try {
            setEnvios(await misEnvios(f))
            setAplicados(f)
            setAbierto(null)
        } catch (err) {
            setError(err.message || 'No se pudo cargar tu historial')
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => { cargar(FILTROS_VACIOS) }, [cargar])

    function onChange(e) {
        setFiltros((prev) => ({ ...prev, [e.target.name]: e.target.value }))
    }

    function filtrar(e) {
        e.preventDefault()
        const errs = validarFiltros(filtros)
        setErrores(errs)
        if (Object.keys(errs).length === 0) cargar(filtros)
    }

    function limpiar() {
        setFiltros(FILTROS_VACIOS)
        setErrores({})
        cargar(FILTROS_VACIOS)
    }

    return (
        <AppShell>
            <div className="page-head">
                <h1>Mis envíos</h1>
                <Link className="btn btn-inline" to="/envios/nuevo">+ Nuevo envío</Link>
            </div>

            <section className="panel">
                <form className="filters filters-historial" onSubmit={filtrar} noValidate>
                    <SelectField label="Estado" name="estado" value={filtros.estado} onChange={onChange}
                                 options={OPCIONES_ESTADO} />
                    <Field label="Desde" name="desde" type="date" value={filtros.desde} onChange={onChange}
                           error={errores.desde} />
                    <Field label="Hasta" name="hasta" type="date" value={filtros.hasta} onChange={onChange}
                           error={errores.hasta} />
                    <button className="btn btn-inline" type="submit" disabled={loading}>Filtrar</button>
                    <button className="btn-sm" type="button" onClick={limpiar}
                            disabled={loading || (!hayFiltros(filtros) && !hayFiltros(aplicados))}>Limpiar</button>
                </form>
            </section>

            {error && <p className="banner banner-error" role="alert">{error}</p>}

            <section className="panel">
                <div className="table-wrap">
                    <table className="users-table historial-table">
                        <thead>
                        <tr>
                            <th>Código</th><th>Fecha</th><th>Origen → Destino</th><th>Estado</th><th>Precio</th><th></th>
                        </tr>
                        </thead>
                        <tbody>
                        {envios.map((e) => (
                            <Fragment key={e.codigoSeguimiento}>
                                <tr>
                                    <td><span className="codigo-mono">{e.codigoSeguimiento}</span></td>
                                    <td>{formatearFecha(e.createdAt)}</td>
                                    <td>{e.origenDireccion} → {e.destinoDireccion}</td>
                                    <td><span className={claseEstado(e.estado)}>{etiquetaEstado(e.estado)}</span></td>
                                    <td>{formatearPrecio(e.precioCotizado)}</td>
                                    <td>
                                        <button className="btn-sm" type="button" aria-expanded={abierto === e.codigoSeguimiento}
                                                onClick={() => setAbierto(abierto === e.codigoSeguimiento ? null : e.codigoSeguimiento)}>
                                            {abierto === e.codigoSeguimiento ? 'Ocultar' : 'Ver detalle'}
                                        </button>
                                    </td>
                                </tr>
                                {abierto === e.codigoSeguimiento && (
                                    <tr className="detalle-fila">
                                        <td colSpan="6">
                                            <dl className="detalle">
                                                <Dato etiqueta="Receptor" valor={e.receptorNombre} />
                                                <Dato etiqueta="Teléfono" valor={e.receptorTelefono} />
                                                <Dato etiqueta="Correo del receptor" valor={e.receptorEmail} />
                                                <Dato etiqueta="Descripción" valor={e.descripcion} />
                                                <Dato etiqueta="Peso" valor={e.pesoKg != null ? `${e.pesoKg} kg` : ''} />
                                                <Dato etiqueta="Medidas"
                                                      valor={e.largoCm != null ? `${e.largoCm} × ${e.anchoCm} × ${e.altoCm} cm` : ''} />
                                                <Dato etiqueta="Distancia" valor={e.distanciaKm != null ? `${e.distanciaKm} km` : ''} />
                                                <Dato etiqueta="Frágil" valor={e.esFragil ? 'Sí' : 'No'} />
                                                <Dato etiqueta="Urgente" valor={e.esUrgente ? 'Sí' : 'No'} />
                                            </dl>
                                        </td>
                                    </tr>
                                )}
                            </Fragment>
                        ))}
                        {!loading && !error && envios.length === 0 && (
                            <tr>
                                <td colSpan="6" className="empty">
                                    {hayFiltros(aplicados)
                                        ? 'No hay envíos que cumplan esos filtros.'
                                        : 'Todavía no has solicitado ningún envío.'}
                                </td>
                            </tr>
                        )}
                        {loading && <tr><td colSpan="6" className="empty">Cargando tu historial…</td></tr>}
                        </tbody>
                    </table>
                </div>
                {!loading && !error && envios.length > 0 && (
                    <p className="historial-total">
                        {envios.length} {envios.length === 1 ? 'envío' : 'envíos'}
                        {hayFiltros(aplicados) ? ' con los filtros aplicados' : ''}
                    </p>
                )}
            </section>
        </AppShell>
    )
}
