import { useEffect, useRef, useState } from 'react'
import { buscarLugares, MIN_CARACTERES } from '../lugaresApi.js'
import '../geocodificacion.css'

const ESPERA_MS = 400 // se busca cuando la persona deja de escribir, no en cada tecla

/**
 * Buscador de lugares con autocompletado: la persona escribe un barrio, direccion o sitio conocido y
 * elige de la lista. Cada opcion muestra solo lo importante: el nombre y, debajo, el barrio y la ciudad.
 * Al elegir se avisa con onSelect({ etiqueta, titulo, subtitulo, lat, lng, textoBuscado }); "Cambiar" avisa con null.
 *
 * Props: etiqueta ("Origen"), name ("origen"), textoInicial (opcional: la direccion ya escrita en otro campo),
 * lat y lng (los que ya tiene el padre: si ambos existen se muestra el lugar como fijado),
 * error (hay un error de coordenadas en este lugar) y onSelect(lugar | null).
 */
export default function AddressSearch({ etiqueta, name, textoInicial, lat, lng, error, onSelect }) {
    // null = la persona aun no ha escrito aqui, asi que se muestra la direccion del otro campo (si hay).
    const [escrito, setEscrito] = useState(null)
    const texto = escrito ?? textoInicial ?? ''
    const [resultados, setResultados] = useState([])
    const [activo, setActivo] = useState(-1)
    const [buscando, setBuscando] = useState(false)
    const [mensaje, setMensaje] = useState('')
    const [elegida, setElegida] = useState(null)
    const temporizador = useRef(null)
    const control = useRef(null)

    const id = `geo-${name}`
    const fijada = String(lat ?? '').trim() !== '' && String(lng ?? '').trim() !== ''

    useEffect(() => () => { clearTimeout(temporizador.current); control.current?.abort() }, [])

    function programarBusqueda(valor) {
        clearTimeout(temporizador.current)
        control.current?.abort()
        setResultados([])
        setActivo(-1)
        setMensaje('')
        if (valor.trim().length < MIN_CARACTERES) { setBuscando(false); return }
        setBuscando(true)
        temporizador.current = setTimeout(() => buscar(valor), ESPERA_MS)
    }

    async function buscar(valor) {
        const c = new AbortController()
        control.current = c
        try {
            const lugares = await buscarLugares(valor, { signal: c.signal })
            setResultados(lugares)
            setMensaje(lugares.length ? '' : 'No encontramos ese lugar. Prueba con el barrio o con un sitio conocido cercano.')
            setBuscando(false)
        } catch (e) {
            if (e.name === 'AbortError') return // llego una busqueda mas nueva
            setMensaje(e.status === 503 ? e.message : 'No pudimos consultar el mapa. Revisa tu conexión e inténtalo de nuevo.')
            setBuscando(false)
        }
    }

    function alEscribir(e) {
        setEscrito(e.target.value)
        programarBusqueda(e.target.value)
    }

    function elegir(lugar) {
        const etiquetaLugar = [lugar.titulo, lugar.subtitulo].filter(Boolean).join(' · ')
        setElegida({ titulo: lugar.titulo, subtitulo: lugar.subtitulo })
        setResultados([])
        setActivo(-1)
        setMensaje('')
        onSelect({ ...lugar, etiqueta: etiquetaLugar, textoBuscado: texto.trim() })
    }

    function cambiar() {
        setElegida(null)
        setEscrito('')
        onSelect(null)
    }

    function alPulsarTecla(e) {
        if (e.key === 'ArrowDown' && resultados.length) {
            e.preventDefault()
            setActivo((i) => (i + 1) % resultados.length)
        } else if (e.key === 'ArrowUp' && resultados.length) {
            e.preventDefault()
            setActivo((i) => (i <= 0 ? resultados.length - 1 : i - 1))
        } else if (e.key === 'Enter') {
            e.preventDefault() // dentro de un formulario, Enter no debe enviarlo
            if (resultados.length) elegir(resultados[activo >= 0 ? activo : 0])
        } else if (e.key === 'Escape') {
            setResultados([])
            setActivo(-1)
        }
    }

    return (
        <div className="field">
            <label htmlFor={id}>{etiqueta}</label>
            {fijada ? (
                <div className="geo-ok">
                    <span className="geo-ok-texto">
                        <strong>✓ {elegida?.titulo || `Ubicación ${lat}, ${lng}`}</strong>
                        {elegida?.subtitulo && <small>{elegida.subtitulo}</small>}
                    </span>
                    <button className="btn-ghost" type="button" onClick={cambiar}>Cambiar</button>
                </div>
            ) : (
                <>
                    <div className="geo-combo">
                        <input id={id} name={`buscar-${name}`} type="text" value={texto} autoComplete="off"
                               role="combobox" aria-expanded={resultados.length > 0} aria-controls={`${id}-lista`}
                               aria-autocomplete="list" aria-invalid={error ? 'true' : 'false'}
                               placeholder="Barrio, dirección o lugar (ej. Granada, Universidad del Quindío)"
                               onChange={alEscribir} onKeyDown={alPulsarTecla} />
                        {buscando && <span className="spinner geo-spinner" aria-label="Buscando" />}
                        {resultados.length > 0 && (
                            <ul className="geo-results" id={`${id}-lista`} role="listbox" aria-label={`Resultados para ${name}`}>
                                {resultados.map((l, i) => (
                                    <li key={l.id} role="option" aria-selected={i === activo}>
                                        <button type="button" className={i === activo ? 'is-activo' : ''}
                                                onMouseDown={(e) => e.preventDefault()} onClick={() => elegir(l)}>
                                            <span className="geo-titulo">{l.titulo}</span>
                                            {l.subtitulo && <span className="geo-subtitulo">{l.subtitulo}</span>}
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                    {mensaje && <p className="geo-msg" role="status">{mensaje}</p>}
                    {error && !mensaje && !resultados.length && (
                        <p className="field-error">{`Busca y elige el lugar de ${name}.`}</p>
                    )}
                </>
            )}
        </div>
    )
}
