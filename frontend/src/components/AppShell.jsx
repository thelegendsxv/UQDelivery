import { NavLink, useNavigate } from 'react-router-dom'
import { clearSession, getSession } from '../auth.js'
import { ROLES, ROLE_LABELS } from '../roles.js'
import { Logo } from './AuthShell.jsx'

//HU-02 - Barra superior con menu segun el rol
export default function AppShell({ children, narrow = false }) {
    const navigate = useNavigate()
    const session = getSession()
    const primerNombre = session.nombre.trim().split(/\s+/)[0]

    function logout() {
        clearSession()
        navigate('/login')
    }

    return (
        <div className="page">
            <header className="topbar">
                <Logo />
                <nav className="nav" aria-label="Principal">
                    <NavLink to="/" end>Inicio</NavLink>
                    {session.role === ROLES.ADMINISTRADOR && <NavLink to="/admin/usuarios">Usuarios</NavLink>}
                    {session.role === ROLES.ADMINISTRADOR && <NavLink to="/admin/tarifas">Tarifas</NavLink>}
                    {session.role === ROLES.CLIENTE_EMISOR && <NavLink to="/cotizar">Cotizar</NavLink>}
                    <NavLink to="/perfil">Mi perfil</NavLink>
                </nav>
                <div className="user">
                    <span className="avatar" aria-hidden="true">{primerNombre.charAt(0).toUpperCase()}</span>
                    <div className="user-info">
                        <span className="user-name">{session.nombre}</span>
                        <span className="chip chip-role">{ROLE_LABELS[session.role] ?? session.role}</span>
                    </div>
                    <button className="btn-ghost" onClick={logout}>Cerrar sesión</button>
                </div>
            </header>
            <main className={narrow ? 'page-body narrow' : 'page-body'}>{children}</main>
        </div>
    )
}