export default function SelectField({ label, name, value, onChange, error, options }) {
    const id = `f-${name}`
    return (
        <div className="field">
            <label htmlFor={id}>{label}</label>
            <select
                id={id}
                name={name}
                value={value}
                onChange={onChange}
                aria-invalid={error ? 'true' : 'false'}
                aria-describedby={error ? `${id}-error` : undefined}
            >
                {options.map((o) => (
                    <option key={o.value} value={o.value}>{o.label}</option>
                ))}
            </select>
            {error && <p className="field-error" id={`${id}-error`}>{error}</p>}
        </div>
    )
}