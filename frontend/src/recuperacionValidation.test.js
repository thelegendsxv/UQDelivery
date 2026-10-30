import { describe, it, expect } from 'vitest'
import { validateOlvido, validateReseteo, tokenValido, esEnlaceInvalido } from './recuperacionValidation.js'

const TOKEN = 'AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-abcde'

describe('validateOlvido', () => {
  it('acepta un correo valido', () => expect(validateOlvido({ email: 'ana@mail.com' })).toEqual({}))
  it('exige el correo', () => expect(validateOlvido({ email: '  ' }).email).toBeDefined())
  it('rechaza correo invalido', () => expect(validateOlvido({ email: 'ana' }).email).toBeDefined())
})

describe('validateReseteo', () => {
  const valid = { password: 'NuevaClave99', confirmPassword: 'NuevaClave99' }
  it('acepta datos validos', () => expect(validateReseteo(valid)).toEqual({}))
  it('exige la contrasena', () =>
    expect(validateReseteo({ password: '', confirmPassword: '' }).password).toBeDefined())
  it('rechaza contrasena debil', () =>
    expect(validateReseteo({ password: 'abc', confirmPassword: 'abc' }).password).toBeDefined())
  it('rechaza contrasena sin numero', () =>
    expect(validateReseteo({ password: 'SinNumero', confirmPassword: 'SinNumero' }).password).toBeDefined())
  it('rechaza contrasenas distintas', () =>
    expect(validateReseteo({ ...valid, confirmPassword: 'Otra12345' }).confirmPassword).toBeDefined())
})

describe('tokenValido', () => {
  it('acepta un token de 43 caracteres Base64 URL', () => expect(tokenValido(TOKEN)).toBe(true))
  it('rechaza token vacio o nulo', () => {
    expect(tokenValido('')).toBe(false)
    expect(tokenValido(null)).toBe(false)
  })
  it('rechaza token con caracteres no permitidos', () =>
    expect(tokenValido('<script>alert(1)</script>xxxxxxxxxxxxxxxxxx')).toBe(false))
  it('rechaza token de otra longitud', () => expect(tokenValido(TOKEN.slice(1))).toBe(false))
})

describe('esEnlaceInvalido', () => {
  it('detecta el mensaje de enlace vencido del backend', () =>
    expect(esEnlaceInvalido({ message: 'El enlace de recuperacion no es valido o ya expiro. Solicita uno nuevo', fieldErrors: {} })).toBe(true))
  it('detecta error de campo token', () =>
    expect(esEnlaceInvalido({ message: 'Datos invalidos', fieldErrors: { token: 'x' } })).toBe(true))
  it('no confunde otros errores', () =>
    expect(esEnlaceInvalido({ message: 'La nueva contrasena debe ser diferente a la actual', fieldErrors: {} })).toBe(false))
})
