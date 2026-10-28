import { useState } from 'react'
import { passwordStrength } from '../passwordStrength.js'

export default function PasswordField({ label, name, value, onChange, error, autoComplete, showStrength = false }) {
  const [visible, setVisible] = useState(false)
  const id = `f-${name}`
  const strength = showStrength ? passwordStrength(value) : null

  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <div className="input-wrap">
        <input
          id={id}
          name={name}
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={onChange}
          autoComplete={autoComplete}
          aria-invalid={error ? 'true' : 'false'}
          aria-describedby={error ? `${id}-error` : undefined}
        />
        <button
          type="button"
          className="reveal"
          onClick={() => setVisible((v) => !v)}
          aria-pressed={visible}
          aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
        >
          {visible ? 'Ocultar' : 'Mostrar'}
        </button>
      </div>

      {strength && value && (
        <div className="strength" data-level={strength.score}>
          <div className="strength-bars" aria-hidden="true"><span /><span /><span /><span /></div>
          <p className="strength-text" aria-live="polite">
            <strong>{strength.label}.</strong> {strength.hint}
          </p>
        </div>
      )}

      {error && <p className="field-error" id={`${id}-error`}>{error}</p>}
    </div>
  )
}
