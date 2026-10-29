import { useCallback, useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { cambiarEstado, cambiarRol, crearUsuario, listarUsuarios } from '../usuariosApi.js'
import { getSession } from '../auth.js'
import { ROLES, ROLE_LABELS, ROLE_OPTIONS } from '../roles.js'
import { validarNuevoUsuario } from '../usuariosValidation.js'
import AppShell from '../components/AppShell.jsx'
import Field from '../components/Field.jsx'
import PasswordField from '../components/PasswordField.jsx'
import SelectField from '../components/SelectField.jsx'

const PAGE_SIZE = 10
const NUEVO_VACIO = { nombre: '', email: '', password: '', role: ROLES.REPARTIDOR }
const ESTADOS = [
    { value: '', label: 'Todos' },
    { value: 'true', label: 'Activos' },
    { value: 'false', label: 'Inactivos' },
]

/** HU-02 - Panel del administrador: listar, filtrar, cambiar rol, activar/desactivar y crear usuarios. */
export default function AdminUsers() {
    const yo = getSession()
    const [filtros, setFiltros] = useState({ q: '', rol: '', activo: '' }) // lo que se escribe
    const [aplicados, setAplicados] = useState(filtros) // lo que se busco con "Buscar"
    const [page, setPage] = useState(0)
    const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 })
    const [loading, setLoading] = useState(true)
    const [aviso, setAviso] = useState({ tipo: '', texto: '' })
    const [mostrarNuevo, setMostrarNuevo] = useState(false)
    const [nuevo, setNuevo] = useState(NUEVO_VACIO)
    const [nuevoErrors, setNuevoErrors] = useState({})
    const [denegado, setDenegado] = useState(false)

    const manejarError = useCallback((err, texto) => {
        // Criterio 2: si el servidor responde 403, el rol ya no permite esta pantalla.
        if (err.status === 403) setDenegado(true)
        else setAviso({ tipo: 'error', texto: err.message || texto })
    }, [])

    const cargar = useCallback(async (f, p) => {
        setLoading(true)
        try {
            setData(await listarUsuarios({ ...f, page: p, size: PAGE_SIZE }))
        } catch (err) {
            manejarError(err, 'No se pudo cargar la lista de usuarios.')
        } finally {
            setLoading(false)
        }
    }, [manejarError])

    useEffect(() => { cargar(aplicados, page) }, [aplicados, page, cargar])

    function buscar(e) {
        e.preventDefault()
        setPage(0)
        setAplicados({ ...filtros })
    }

    const reemplazar = (u) =>
        setData((d) => ({ ...d, content: d.content.map((x) => (x.id === u.id ? u : x)) }))

    async function onCambiarRol(u, role) {
        if (role === u.role) return
        if (!window.confirm(`¿Cambiar el rol de ${u.nombre} a ${ROLE_LABELS[role]}?`)) return
        try {
            reemplazar(await cambiarRol(u.id, role))
            setAviso({ tipo: 'ok', texto: `Rol de ${u.nombre} actualizado a ${ROLE_LABELS[role]}.` })
        } catch (err) {
            manejarError(err, 'No se pudo cambiar el rol.')
        }
    }

    async function onCambiarEstado(u) {
        const accion = u.activo ? 'desactivar' : 'activar'
        if (!window.confirm(`¿Seguro que deseas ${accion} a ${u.nombre}?`)) return
        try {
            reemplazar(await cambiarEstado(u.id, !u.activo))
            setAviso({ tipo: 'ok', texto: `${u.nombre} quedó ${u.activo ? 'inactivo' : 'activo'}.` })
        } catch (err) {
            manejarError(err, 'No se pudo cambiar el estado.')
        }
    }

    async function onCrear(e) {
        e.preventDefault()
        const found = validarNuevoUsuario(nuevo)
        setNuevoErrors(found)
        if (Object.keys(found).length) return
        try {
            const u = await crearUsuario({ ...nuevo, nombre: nuevo.nombre.trim(), email: nuevo.email.trim() })
            setAviso({ tipo: 'ok', texto: `Usuario ${u.email} creado como ${ROLE_LABELS[u.role]}.` })
            setNuevo(NUEVO_VACIO)
            setMostrarNuevo(false)
            cargar(aplicados, page)
        } catch (err) {
            setNuevoErrors(err.fieldErrors || {})
            manejarError(err, 'No se pudo crear el usuario.')
        }
    }

    if (denegado) return <Navigate to="/acceso-denegado" replace />

    const onNuevo = (e) => setNuevo({ ...nuevo, [e.target.name]: e.target.value })
    const onFiltro = (e) => setFiltros({ ...filtros, [e.target.name]: e.target.value })

    return (
        <AppShell>
            <div className="page-head">
                <h1>Usuarios y roles</h1>
                <button className="btn btn-inline" onClick={() => setMostrarNuevo((v) => !v)}>
                    {mostrarNuevo ? 'Cerrar' : '+ Nuevo usuario'}
                </button>
            </div>

            {aviso.texto && (
                <p className={`banner ${aviso.tipo === 'ok' ? 'banner-ok' : 'banner-error'}`}
                   role={aviso.tipo === 'ok' ? 'status' : 'alert'}>
                    {aviso.texto}
                </p>
            )}

            {mostrarNuevo && (
                <section className="panel">
                    <h2>Nuevo usuario</h2>
                    <form className="grid-form" onSubmit={onCrear} noValidate>
                        <Field label="Nombre" name="nombre" value={nuevo.nombre} onChange={onNuevo} error={nuevoErrors.nombre} />
                        <Field label="Correo electrónico" name="email" type="email" value={nuevo.email}
                               onChange={onNuevo} error={nuevoErrors.email} autoComplete="off" />
                        <PasswordField label="Contraseña temporal" name="password" value={nuevo.password}
                                       onChange={onNuevo} error={nuevoErrors.password} autoComplete="new-password" showStrength />
                        <SelectField label="Rol" name="role" value={nuevo.role} onChange={onNuevo}
                                     error={nuevoErrors.role} options={ROLE_OPTIONS} />
                        <div className="actions">
                            <button className="btn btn-inline" type="submit">Crear usuario</button>
                        </div>
                    </form>
                </section>
            )}

            <section className="panel">
                <form className="filters" onSubmit={buscar}>
                    <Field label="Buscar por nombre o correo" name="q" value={filtros.q} onChange={onFiltro} />
                    <SelectField label="Rol" name="rol" value={filtros.rol} onChange={onFiltro}
                                 options={[{ value: '', label: 'Todos' }, ...ROLE_OPTIONS]} />
                    <SelectField label="Estado" name="activo" value={filtros.activo} onChange={onFiltro} options={ESTADOS} />
                    <button className="btn btn-inline" type="submit">Buscar</button>
                </form>
            </section>

            <section className="panel">
                <div className="table-wrap">
                    <table className="users-table">
                        <thead>
                        <tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th>Acción</th></tr>
                        </thead>
                        <tbody>
                        {data.content.map((u) => {
                            const esYo = u.email === yo?.email
                            return (
                                <tr key={u.id} className={u.activo ? '' : 'is-off'}>
                                    <td>{u.nombre}{esYo && <span className="chip chip-you">tú</span>}</td>
                                    <td>{u.email}</td>
                                    <td>
                                        <select className="row-select" aria-label={`Rol de ${u.nombre}`} value={u.role}
                                                disabled={esYo} title={esYo ? 'No puedes cambiar tu propio rol' : undefined}
                                                onChange={(e) => onCambiarRol(u, e.target.value)}>
                                            {ROLE_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                                        </select>
                                    </td>
                                    <td>
                      <span className={`chip ${u.activo ? 'chip-on' : 'chip-off'}`}>
                        {u.activo ? 'Activo' : 'Inactivo'}
                      </span>
                                    </td>
                                    <td>
                                        <button className={`btn-sm ${u.activo ? 'danger' : 'ok'}`} disabled={esYo}
                                                onClick={() => onCambiarEstado(u)}>
                                            {u.activo ? 'Desactivar' : 'Activar'}
                                        </button>
                                    </td>
                                </tr>
                            )
                        })}
                        {!loading && data.content.length === 0 && (
                            <tr><td colSpan="5" className="empty">No hay usuarios con esos filtros.</td></tr>
                        )}
                        </tbody>
                    </table>
                </div>

                <div className="pager">
                    <button className="btn-sm" disabled={page === 0 || loading} onClick={() => setPage((p) => p - 1)}>Anterior</button>
                    <span>Página {data.totalPages === 0 ? 0 : page + 1} de {data.totalPages} · {data.totalElements} usuarios</span>
                    <button className="btn-sm" disabled={page + 1 >= data.totalPages || loading} onClick={() => setPage((p) => p + 1)}>Siguiente</button>
                </div>
            </section>
        </AppShell>
    )
}