import { describe, it, expect } from 'vitest'
import { validateRegister, validateLogin } from './validation.js'

const valid = { nombre: 'Ana Perez', email: 'ana@mail.com', password: 'Clave1234', confirmPassword: 'Clave1234' }

describe('validateRegister', () => {
  it('acepta datos validos', () => expect(validateRegister(valid)).toEqual({}))
  it('rechaza correo invalido', () =>
    expect(validateRegister({ ...valid, email: 'ana' }).email).toBeDefined())
  it('rechaza contrasena debil', () =>
    expect(validateRegister({ ...valid, password: 'abc', confirmPassword: 'abc' }).password).toBeDefined())
  it('rechaza contrasenas distintas', () =>
    expect(validateRegister({ ...valid, confirmPassword: 'Otra1234' }).confirmPassword).toBeDefined())
  it('rechaza html en el nombre', () =>
    expect(validateRegister({ ...valid, nombre: '<script>x</script>' }).nombre).toBeDefined())
})

describe('validateLogin', () => {
  it('exige correo y contrasena', () => {
    const e = validateLogin({ email: '', password: '' })
    expect(e.email).toBeDefined()
    expect(e.password).toBeDefined()
  })
  it('acepta datos validos', () =>
    expect(validateLogin({ email: 'ana@mail.com', password: 'x' })).toEqual({}))
})
