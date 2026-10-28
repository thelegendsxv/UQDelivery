import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register } from '../api.js'
import { validateRegister } from '../validation.js'
import AuthShell from '../components/AuthShell.jsx'
import Field from '../components/Field.jsx'
import PasswordField from '../components/PasswordField.jsx'

export default function Register() {
  const navigate = useNavigate()
  const [values, setValues] = useState({ nombre: '', email: '', password: '', confirmPassword: '' })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [loading, setLoading] = useState(false)

  const onChange = (e) => setValues({ ...values, [e.target.name]: e.target.value })

  async function onSubmit(e) {
    e.preventDefault()
    setServerError('')
    const found = validateRegister(values)
    setErrors(found)
    if (Object.keys(found).length) return
    setLoading(true)
    try {
      await register(values)
      navigate('/login', { state: { registered: true } })
    } catch (err) {
      setErrors(err.fieldErrors || {})
      setServerError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell
      title="Crea tu cuenta"
      subtitle="Regístrate para solicitar y seguir tus envíos."
      footer={<>¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link></>}
    >
      <form onSubmit={onSubmit} noValidate>
        <Field label="Nombre completo" name="nombre" value={values.nombre}
               onChange={onChange} error={errors.nombre} autoComplete="name" />
        <Field label="Correo electrónico" name="email" type="email" value={values.email}
               onChange={onChange} error={errors.email} autoComplete="email" />
        <PasswordField label="Contraseña" name="password" value={values.password}
                       onChange={onChange} error={errors.password} autoComplete="new-password" showStrength />
        <PasswordField label="Confirma tu contraseña" name="confirmPassword" value={values.confirmPassword}
                       onChange={onChange} error={errors.confirmPassword} autoComplete="new-password" />
        {serverError && <p className="banner banner-error" role="alert">{serverError}</p>}
        <button className="btn" type="submit" disabled={loading}>
          {loading ? <><span className="spinner" aria-hidden="true" />Creando cuenta…</> : 'Crear cuenta'}
        </button>
      </form>
    </AuthShell>
  )
}
