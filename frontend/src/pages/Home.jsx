import { useNavigate } from 'react-router-dom'
import { clearSession, getSession } from '../auth.js'
import { Logo } from '../components/AuthShell.jsx'
import RouteMap from '../components/RouteMap.jsx'

const ROLES = {
  ADMINISTRADOR: {
    nombre: 'administrador',
    acciones: [
      ['Gestionar usuarios', 'Administra repartidores y clientes corporativos.'],
      ['Configurar la plataforma', 'Ajusta la configuración global del sistema.'],
      ['Ver métricas del negocio', 'Consulta el rendimiento general de los envíos.'],
    ],
  },
  CLIENTE_EMISOR: {
    nombre: 'cliente emisor',
    acciones: [
      ['Solicitar un envío', 'Registra un paquete con su origen y su destino.'],
      ['Pagar el servicio', 'Paga tus envíos desde la plataforma.'],
      ['Consultar tu historial', 'Revisa todos los envíos que has solicitado.'],
    ],
  },
  REPARTIDOR: {
    nombre: 'repartidor',
    acciones: [
      ['Ver tus entregas', 'Consulta la lista de paquetes que te asignaron.'],
      ['Actualizar el estado', 'Marca cada paquete a medida que avanza.'],
      ['Navegar por el mapa', 'Sigue la ruta hasta el destino.'],
    ],
  },
}

export default function Home() {
  const navigate = useNavigate()
  const session = getSession()
  const rol = ROLES[session.role] ?? { nombre: session.role, acciones: [] }
  const primerNombre = session.nombre.trim().split(/\s+/)[0]

  function logout() {
    clearSession()
    navigate('/login')
  }

  // React escapa el contenido por defecto, así que el nombre no puede inyectar HTML.
  return (
    <div className="home">
      <header className="topbar">
        <Logo />
        <div className="user">
          <span className="avatar" aria-hidden="true">{primerNombre.charAt(0).toUpperCase()}</span>
          <div className="user-info">
            <span className="user-name">{session.nombre}</span>
            <span className="chip chip-role">{rol.nombre}</span>
          </div>
          <button className="btn-ghost" onClick={logout}>Cerrar sesión</button>
        </div>
      </header>

      <section className="home-hero">
        <RouteMap className="map-svg" />
        <div className="hero-fade" />
        <div className="hero-text">
          <h1>Hola, {primerNombre}.</h1>
          <p>Tu cuenta está lista. Entraste como {rol.nombre}.</p>
        </div>
      </section>

      <main className="home-body">
        <h2>Lo que podrás hacer como {rol.nombre}</h2>
        <ul className="todo">
          {rol.acciones.map(([titulo, detalle]) => (
            <li key={titulo}>
              <div>
                <p className="todo-title">{titulo}</p>
                <p className="todo-desc">{detalle}</p>
              </div>
              <span className="chip chip-soon">Próximamente</span>
            </li>
          ))}
        </ul>
        <p className="home-note">Estas secciones se irán activando a medida que avance el proyecto.</p>
      </main>
    </div>
  )
}
