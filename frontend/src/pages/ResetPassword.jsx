import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { restablecerContrasena } from '../recuperacionApi.js'
import { esEnlaceInvalido, tokenValido, validateReseteo } from '../recuperacionValidation.js'
import AuthShell from '../components/AuthShell.jsx'
import PasswordField from '../components/PasswordField.jsx'
import '../recuperacion.css'

// HU-03 - Paso 2: crear la contrasena nueva desde el enlace del correo (/reset-password?token=...).
export default function ResetPassword() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const token = params.get('token')
  const [values, setValues] = useState({ password: '', confirmPassword: '' })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [loading, setLoading] = useState(false)
  const [enlaceInvalido, setEnlaceInvalido] = useState(!tokenValido(token))

  const onChange = (e) => setValues({ ...values, [e.target.name]: e.target.value })

  async function onSubmit(e) {
    e.preventDefault()
    setServerError('')
    const found = validateReseteo(values)
    setErrors(found)
    if (Object.keys(found).length) return
    setLoading(true)
    try {
      await restablecerContrasena({ token, password: values.password })
      navigate('/login', { replace: true, state: { passwordReset: true } })
    } catch (err) {
      if (esEnlaceInvalido(err)) {
        setEnlaceInvalido(true)
      } else {
        setErrors(err.fieldErrors || {})
        setServerError(err.message)
      }
    } finally {
      setLoading(false)
    }
  }

  const footer = <>¿La recordaste? <Link to="/login">Inicia sesión</Link></>

  if (enlaceInvalido) {
    return (
      <AuthShell
        title="Este enlace ya no sirve"
        subtitle="El enlace no es válido, venció o ya se usó. Pide uno nuevo y úsalo en los próximos 30 minutos."
        footer={footer}
      >
        <Link className="btn" to="/recuperar-contrasena">Pedir un enlace nuevo</Link>
      </AuthShell>
    )
  }

  return (
    <AuthShell
      title="Crea tu contraseña nueva"
      subtitle="Elige una contraseña distinta a la anterior. Al guardarla podrás iniciar sesión con ella."
      footer={footer}
    >
      <form onSubmit={onSubmit} noValidate>
        <PasswordField label="Contraseña nueva" name="password" value={values.password}
                       onChange={onChange} error={errors.password} autoComplete="new-password" showStrength />
        <PasswordField label="Confirma la contraseña nueva" name="confirmPassword" value={values.confirmPassword}
                       onChange={onChange} error={errors.confirmPassword} autoComplete="new-password" />
        {serverError && <p className="banner banner-error" role="alert">{serverError}</p>}
        <button className="btn" type="submit" disabled={loading}>
          {loading ? <><span className="spinner" aria-hidden="true" />Guardando…</> : 'Guardar contraseña'}
        </button>
      </form>
    </AuthShell>
  )
}
