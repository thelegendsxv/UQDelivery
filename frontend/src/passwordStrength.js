// Mide la fortaleza de una contraseña para guiar a la persona mientras escribe.
export function passwordStrength(pw) {
  if (!pw) return { score: 0, label: '', hint: '' }

  const longEnough = pw.length >= 8
  const mixedCase = /[a-z]/.test(pw) && /[A-Z]/.test(pw)
  const hasDigit = /\d/.test(pw)
  const extra = pw.length >= 12 || /[^A-Za-z0-9]/.test(pw)

  const score = Math.max(1, [longEnough, mixedCase, hasDigit, extra].filter(Boolean).length)
  const label = ['', 'Débil', 'Aceptable', 'Buena', 'Fuerte'][score]

  let hint
  if (!longEnough) hint = 'Usa al menos 8 caracteres.'
  else if (!mixedCase) hint = 'Combina mayúsculas y minúsculas.'
  else if (!hasDigit) hint = 'Agrega un número.'
  else if (!extra) hint = 'Suma un símbolo o más longitud para hacerla más fuerte.'
  else hint = 'Lista para usar.'

  return { score, label, hint }
}
