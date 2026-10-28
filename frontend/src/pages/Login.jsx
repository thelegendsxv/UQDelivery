import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { login } from '../api.js'
import { saveSession } from '../auth.js'
import { validateLogin } from '../validation.js'
import AuthShell from '../components/AuthShell.jsx'
import Field from '../components/Field.jsx'
import PasswordField from '../components/PasswordField.jsx'

export default function Login() {
  const navigate = useNavigate()
  const location = useLocation()
  const [values, setValues] = useState({ email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [loading, setLoading] = useState(false)

  const onChange = (e) => setValues({ ...values, [e.target.name]: e.target.value })

  async function onSubmit(e) {
    e.preventDefault()
    setServerError('')
    const found = validateLogin(values)
    setErrors(found)
    if (Object.keys(found).length) return
    setLoading(true)
    try {
      saveSession(await login(values))
      navigate('/')
    } catch (err) {
      setServerError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      title="Inicia sesión"
      subtitle="Entra con tu correo y tu contraseña."
      footer={<>¿Aún no tienes cuenta? <Link to="/registro">Crea una</Link></>}
    >
      {location.state?.registered && (
        <p className="banner banner-ok" role="status">Cuenta creada. Ya puedes iniciar sesión.</p>
      )}
      <form onSubmit={onSubmit} noValidate>
        <Field label="Correo electrónico" name="email" type="email" value={values.email}
               onChange={onChange} error={errors.email} autoComplete="email" />
        <PasswordField label="Contraseña" name="password" value={values.password}
                       onChange={onChange} error={errors.password} autoComplete="current-password" />
        {serverError && <p className="banner banner-error" role="alert">{serverError}</p>}
        <button className="btn" type="submit" disabled={loading}>
          {loading ? <><span className="spinner" aria-hidden="true" />Entrando…</> : 'Iniciar sesión'}
        </button>
      </form>
    </AuthShell>
  )
}
