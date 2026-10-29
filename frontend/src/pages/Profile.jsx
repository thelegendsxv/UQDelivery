import { useEffect, useState } from 'react'
import { actualizarMiPerfil, miPerfil } from '../usuariosApi.js'
import { getSession, saveSession } from '../auth.js'
import { ROLE_LABELS } from '../roles.js'
import { validarPerfil } from '../usuariosValidation.js'
import AppShell from '../components/AppShell.jsx'
import Field from '../components/Field.jsx'

const aFormulario = (p) => ({ nombre: p.nombre ?? '', telefono: p.telefono ?? '', direccion: p.direccion ?? '' })

/** Si el administrador cambio el rol o el nombre, la sesion guardada se actualiza */
function sincronizarSesion(p) {
    const s = getSession()
    if (s && (s.role !== p.role || s.nombre !== p.nombre)) saveSession({ ...s, nombre: p.nombre, role: p.role })
}

/** HU-02 - El usuario ve sus datos y edita nombre, telefono y direccion. */
export default function Profile() {
    const [perfil, setPerfil] = useState(null)
    const [values, setValues] = useState({ nombre: '', telefono: '', direccion: '' })
    const [errors, setErrors] = useState({})
    const [aviso, setAviso] = useState({ tipo: '', texto: '' })
    const [editando, setEditando] = useState(false)
    const [saving, setSaving] = useState(false)

    useEffect(() => {
        miPerfil()
            .then((p) => {
                sincronizarSesion(p)
                setPerfil(p)
                setValues(aFormulario(p))
            })
            .catch((err) => setAviso({ tipo: 'error', texto: err.message || 'No se pudo cargar tu perfil.' }))
    }, [])

    const onChange = (e) => setValues({ ...values, [e.target.name]: e.target.value })

    async function onSubmit(e) {
        e.preventDefault()
        const found = validarPerfil(values)
        setErrors(found)
        if (Object.keys(found).length) return
        setSaving(true)
        try {
            const p = await actualizarMiPerfil({
                nombre: values.nombre.trim(),
                telefono: values.telefono.trim(),
                direccion: values.direccion.trim(),
            })
            sincronizarSesion(p)
            setPerfil(p)
            setEditando(false)
            setAviso({ tipo: 'ok', texto: 'Tus datos se guardaron.' })
        } catch (err) {
            setErrors(err.fieldErrors || {})
            setAviso({ tipo: 'error', texto: err.message || 'No se pudieron guardar los cambios.' })
        } finally {
            setSaving(false)
        }
    }

    function cancelar() {
        setEditando(false)
        setErrors({})
        setValues(aFormulario(perfil))
    }

    return (
        <AppShell narrow>
            <div className="page-head"><h1>Mi perfil</h1></div>

            {aviso.texto && (
                <p className={`banner ${aviso.tipo === 'ok' ? 'banner-ok' : 'banner-error'}`}
                   role={aviso.tipo === 'ok' ? 'status' : 'alert'}>
                    {aviso.texto}
                </p>
            )}

            {!perfil ? (
                !aviso.texto && <p className="lede">Cargando…</p>
            ) : (
                <section className="panel">
                    {!editando ? (
                        <>
                            <dl className="details">
                                <dt>Nombre</dt><dd>{perfil.nombre}</dd>
                                <dt>Correo</dt><dd>{perfil.email}</dd>
                                <dt>Rol</dt><dd><span className="chip chip-role">{ROLE_LABELS[perfil.role] ?? perfil.role}</span></dd>
                                <dt>Teléfono</dt><dd>{perfil.telefono || '—'}</dd>
                                <dt>Dirección</dt><dd>{perfil.direccion || '—'}</dd>
                            </dl>
                            <button className="btn btn-inline" onClick={() => { setAviso({ tipo: '', texto: '' }); setEditando(true) }}>
                                Editar mis datos
                            </button>
                        </>
                    ) : (
                        <form onSubmit={onSubmit} noValidate>
                            <Field label="Nombre" name="nombre" value={values.nombre} onChange={onChange}
                                   error={errors.nombre} autoComplete="name" />
                            <div className="readonly">
                                <span>Correo electrónico (no se puede cambiar)</span>
                                <p>{perfil.email}</p>
                            </div>
                            <Field label="Teléfono" name="telefono" type="tel" value={values.telefono} onChange={onChange}
                                   error={errors.telefono} autoComplete="tel" />
                            <Field label="Dirección" name="direccion" value={values.direccion} onChange={onChange}
                                   error={errors.direccion} autoComplete="street-address" />
                            <div className="actions">
                                <button className="btn btn-inline" type="submit" disabled={saving}>
                                    {saving ? <><span className="spinner" aria-hidden="true" />Guardando…</> : 'Guardar'}
                                </button>
                                <button className="btn-ghost" type="button" onClick={cancelar}>Cancelar</button>
                            </div>
                        </form>
                    )}
                </section>
            )}
        </AppShell>
    )
}