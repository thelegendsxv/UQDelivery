import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { solicitarRecuperacion } from '../recuperacionApi.js'
import { validateOlvido } from '../recuperacionValidation.js'
import AuthShell from '../components/AuthShell.jsx'
import Field from '../components/Field.jsx'
import '../recuperacion.css'

// HU-03 - Paso 1: pedir el enlace de recuperacion.
// La respuesta es la misma exista o no el correo, asi que la pantalla tampoco lo revela.
export default function ForgotPassword() {
  const location = useLocation()
  const [values, setValues] = useState({ email: location.state?.email || '' })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [loading, setLoading] = useState(false)
  const [enviado, setEnviado] = useState(false)
  const [reenviado, setReenviado] = useState(false)

  const onChange = (e) => setValues({ ...values, [e.target.name]: e.target.value })

  async function enviar() {
    setServerError('')
    setLoading(true)
    try {
      await solicitarRecuperacion(values.email)
      return true
    } catch (err) {
      setErrors(err.fieldErrors || {})
      setServerError(err.message)
      return false
    } finally {
      setLoading(false)
    }
  }

  async function onSubmit(e) {
    e.preventDefault()
    const found = validateOlvido(values)
    setErrors(found)
    if (Object.keys(found).length) return
    if (await enviar()) setEnviado(true)
  }

  async function onReenviar() {
    setReenviado(false)
    if (await enviar()) setReenviado(true)
  }

  const footer = <>¿Ya la recordaste? <Link to="/login">Inicia sesión</Link></>

  if (enviado) {
    return (
      <AuthShell
        title="Revisa tu correo"
        subtitle="Si el correo está registrado, te enviamos un enlace para crear una contraseña nueva."
        footer={footer}
      >
        <p className="sent-to"><span>Enviado a</span><strong>{values.email.trim()}</strong></p>
        <ol className="recovery-steps">
          <li>Abre el correo de UQDelivery. Si no lo ves, revisa la carpeta de spam.</li>
          <li>Abre el enlace. Vence en 30 minutos y sirve una sola vez.</li>
          <li>Crea tu contraseña nueva e inicia sesión con ella.</li>
        </ol>
        {reenviado && <p className="banner banner-ok" role="status">Te enviamos otro enlace. El anterior ya no sirve.</p>}
        {serverError && <p className="banner banner-error" role="alert">{serverError}</p>}
        <button className="btn-ghost btn-block" type="button" onClick={onReenviar} disabled={loading}>
          {loading ? 'Enviando…' : 'Enviar otro enlace'}
        </button>
      </AuthShell>
    )
  }

  return (
    <AuthShell
      title="Recupera tu contraseña"
      subtitle="Escribe el correo de tu cuenta y te enviaremos un enlace para crear una contraseña nueva."
      footer={footer}
    >
      <form onSubmit={onSubmit} noValidate>
        <Field label="Correo electrónico" name="email" type="email" value={values.email}
               onChange={onChange} error={errors.email} autoComplete="email" />
        {serverError && <p className="banner banner-error" role="alert">{serverError}</p>}
        <button className="btn" type="submit" disabled={loading}>
          {loading ? <><span className="spinner" aria-hidden="true" />Enviando…</> : 'Enviar enlace'}
        </button>
      </form>
    </AuthShell>
  )
}
