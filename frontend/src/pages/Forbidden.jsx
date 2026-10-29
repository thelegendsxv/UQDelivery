import { Link } from 'react-router-dom'
import AppShell from '../components/AppShell.jsx'

/** HU-02 - criterio 2: se muestra cuando el rol no permite entrar a una funcionalidad */
export default function Forbidden() {
    return (
        <AppShell>
            <section className="denied" role="alert">
                <h1>Acceso denegado</h1>
                <p>Tu rol no tiene permiso para usar esta sección. Si crees que es un error, pide al administrador que revise tu rol.</p>
                <Link className="btn" to="/">Volver al inicio</Link>
            </section>
        </AppShell>
    )
}