import { useEffect, useState } from 'react'

// Ruta del paquete: sigue las calles del mapa (ángulos rectos, como en una ciudad en cuadrícula).
const ROUTE = 'M120 760 L120 620 L330 620 L330 470 L560 470 L560 260 L700 260 L700 140'
const VERTICALES = [60, 120, 200, 330, 440, 560, 640, 700, 770]
const HORIZONTALES = [80, 140, 260, 350, 470, 540, 620, 700, 760, 840]
const ARBOLES = [[380, 520], [425, 565], [470, 522], [512, 572], [366, 582], [500, 505]]

function usePrefersReducedMotion() {
  const query = '(prefers-reduced-motion: reduce)'
  const [reduced, setReduced] = useState(
    () => typeof window !== 'undefined' && !!window.matchMedia?.(query).matches,
  )
  useEffect(() => {
    if (!window.matchMedia) return undefined
    const mq = window.matchMedia(query)
    const onChange = () => setReduced(mq.matches)
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [])
  return reduced
}

/** Plano decorativo de ciudad con la ruta de un envío que se dibuja al cargar. */
export default function RouteMap({ className = '' }) {
  const reduced = usePrefersReducedMotion()

  return (
    <svg
      className={`route-map ${reduced ? 'is-static' : ''} ${className}`}
      viewBox="0 0 830 920"
      preserveAspectRatio="xMidYMid slice"
      aria-hidden="true"
      focusable="false"
    >
      <rect width="830" height="920" fill="var(--map-ground)" />

      {/* río */}
      <path d="M-40 330 C 180 420, 420 230, 880 390" fill="none"
            stroke="var(--map-water)" strokeWidth="78" strokeLinecap="round" />

      {/* parque */}
      <rect x="345" y="485" width="200" height="120" rx="14" fill="var(--map-park)" />
      {ARBOLES.map(([x, y]) => (
        <circle key={`${x}-${y}`} cx={x} cy={y} r="9" fill="var(--map-tree)" />
      ))}

      {/* calles */}
      <g stroke="var(--map-street)" strokeLinecap="round">
        {VERTICALES.map((x) => (
          <line key={`v${x}`} x1={x} y1="-10" x2={x} y2="930" strokeWidth={x === 330 ? 22 : 12} />
        ))}
        {HORIZONTALES.map((y) => (
          <line key={`h${y}`} x1="-10" y1={y} x2="840" y2={y} strokeWidth={y === 470 ? 22 : 12} />
        ))}
      </g>


      {/* ruta: halo + trazo que se dibuja */}
      <path d={ROUTE} pathLength="1" className="route-line route-halo" fill="none" strokeLinecap="round" strokeLinejoin="round" />
      <path d={ROUTE} pathLength="1" className="route-line route-main" fill="none" strokeLinecap="round" strokeLinejoin="round" />

      {/* origen */}
      <circle cx="120" cy="760" r="12" fill="#fff" stroke="var(--route)" strokeWidth="6" />

      {/* destino */}
      <g transform="translate(700 140)">
        <g className="pin-drop">
          <path d="M0 0 C-10 -14 -22 -28 -22 -46 A22 22 0 0 1 22 -46 C22 -28 10 -14 0 0Z"
                fill="var(--saffron)" stroke="var(--ink)" strokeWidth="3" strokeLinejoin="round" />
          <circle cy="-46" r="8" fill="var(--ink)" />
        </g>
      </g>

      {/* paquete en movimiento */}
      {reduced ? (
        <g transform="translate(330 470)">
          <Parcel />
        </g>
      ) : (
        <g opacity="0">
          <set attributeName="opacity" to="1" begin="2.8s" fill="freeze" />
          <animateMotion dur="11s" begin="2.8s" repeatCount="indefinite" path={ROUTE} />
          <Parcel />
        </g>
      )}
    </svg>
  )
}

function Parcel() {
  return (
    <g>
      <rect x="-14" y="-14" width="28" height="28" rx="6" fill="var(--saffron)" stroke="var(--ink)" strokeWidth="3" />
      <path d="M-14 -2 H14 M0 -14 V14" stroke="var(--ink)" strokeWidth="2.5" />
    </g>
  )
}
