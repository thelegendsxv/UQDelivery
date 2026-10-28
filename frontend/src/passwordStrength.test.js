import { describe, it, expect } from 'vitest'
import { passwordStrength } from './passwordStrength.js'

describe('passwordStrength', () => {
  it('no mide nada si está vacía', () => expect(passwordStrength('').score).toBe(0))
  it('marca como débil una contraseña corta', () => {
    const r = passwordStrength('abc')
    expect(r.score).toBe(1)
    expect(r.label).toBe('Débil')
  })
  it('marca como buena una que cumple las reglas del servidor', () =>
    expect(passwordStrength('Clave1234').score).toBe(3))
  it('marca como fuerte una larga con símbolo', () =>
    expect(passwordStrength('Clave1234!xyz').score).toBe(4))
  it('indica qué falta', () =>
    expect(passwordStrength('clavesinnumero').hint).toBe('Combina mayúsculas y minúsculas.'))
})
