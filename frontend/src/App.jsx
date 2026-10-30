import { Navigate, Route, Routes } from 'react-router-dom'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import Home from './pages/Home.jsx'
import Profile from './pages/Profile.jsx'
import AdminUsers from './pages/AdminUsers.jsx'
import Forbidden from './pages/Forbidden.jsx'
import Quote from './pages/Quote.jsx'
import AdminTarifas from './pages/AdminTarifas.jsx'
import NewShipment from './pages/NewShipment.jsx'
import { getSession } from './auth.js'
import { ROLES } from './roles.js'
import './usuarios.css'

function Protected({ children, roles }) {
    const session = getSession()
    if (!session) return <Navigate to="/login" replace />
    if (roles && !roles.includes(session.role)) return <Navigate to="/acceso-denegado" replace />
    return children
}

export default function App() {
    return (
        <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/registro" element={<Register />} />
            <Route path="/" element={<Protected><Home /></Protected>} />
            <Route path="/perfil" element={<Protected><Profile /></Protected>} />
            <Route path="/admin/usuarios" element={<Protected roles={[ROLES.ADMINISTRADOR]}><AdminUsers /></Protected>} />
            <Route path="/cotizar" element={<Protected roles={[ROLES.CLIENTE_EMISOR]}><Quote /></Protected>} />
            <Route path="/envios/nuevo" element={<Protected roles={[ROLES.CLIENTE_EMISOR]}><NewShipment /></Protected>} />
            <Route path="/admin/tarifas" element={<Protected roles={[ROLES.ADMINISTRADOR]}><AdminTarifas /></Protected>} />
            <Route path="/acceso-denegado" element={<Protected><Forbidden /></Protected>} />
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    )
}