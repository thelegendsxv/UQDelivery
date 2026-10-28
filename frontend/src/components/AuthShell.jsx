import RouteMap from './RouteMap.jsx'

export function Logo() {
  return (
    <div className="logo">
      <svg viewBox="0 0 32 32" width="36" height="36" aria-hidden="true" focusable="false">
        <rect width="32" height="32" rx="8" fill="var(--saffron)" />
        <path d="M8 23v-7h8V9h8" fill="none" stroke="var(--ink)" strokeWidth="3"
              strokeLinecap="round" strokeLinejoin="round" />
      </svg>
      <span className="logo-text">UQDelivery</span>
    </div>
  )
}

function Ticket() {
  return (
    <div className="ticket" aria-hidden="true">
      <div className="ticket-head">
        <span className="ticket-code">UQ-48213</span>
        <span className="chip chip-live"><i />En ruta</span>
      </div>
      <ol className="ticket-steps">
        <li className="is-done">Recogido</li>
        <li className="is-current">En camino</li>
        <li>Entregado</li>
      </ol>
      <div className="ticket-eta">
        <span>Llegada estimada</span>
        <strong>12 min</strong>
      </div>
      <div className="barcode" />
    </div>
  )
}

export default function AuthShell({ title, subtitle, footer, children }) {
  return (
    <div className="auth">
      <aside className="map-panel">
        <RouteMap className="map-svg" />
        <div className="map-fade" />
        <div className="map-top">
          <Logo />
          <h2 className="map-headline">Cada paquete sabe a dónde va.</h2>
          <p className="map-sub">Registra tus envíos, asígnalos a un repartidor y síguelos en tiempo real.</p>
        </div>
        <Ticket />
      </aside>

      <main className="form-panel">
        <div className="form-wrap">
          <h1>{title}</h1>
          <p className="lede">{subtitle}</p>
          {children}
          <p className="switch">{footer}</p>
        </div>
      </main>
    </div>
  )
}
