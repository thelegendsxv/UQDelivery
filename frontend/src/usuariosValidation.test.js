import { describe, it, expect } from 'vitest'
import { validarNuevoUsuario, validarPerfil } from './usuariosValidation.js'

describe('validarPerfil', () => {
    it('acepta un perfil valido con campos opcionales vacios', () =>
        expect(validarPerfil({ nombre: 'Sara Bolaños', telefono: '', direccion: '' })).toEqual({}))
    it('exige el nombre y rechaza html', () => {
        expect(validarPerfil({ nombre: '' }).nombre).toBeDefined()
        expect(validarPerfil({ nombre: '<script>x</script>' }).nombre).toBeDefined()
        expect(validarPerfil({ nombre: 'Ana', direccion: '<img src=x>' }).direccion).toBeDefined()
    })
    it('valida el telefono', () => {
        expect(validarPerfil({ nombre: 'Ana', telefono: 'abc' }).telefono).toBeDefined()
        expect(validarPerfil({ nombre: 'Ana', telefono: '+57 300 123 4567' })).toEqual({})
    })
})

describe('validarNuevoUsuario', () => {
    it('acepta un repartidor valido', () =>
        expect(validarNuevoUsuario({ nombre: 'Pedro', email: 'p@mail.com', password: 'Clave1234', role: 'REPARTIDOR' })).toEqual({}))
    it('reporta todos los errores', () => {
        const e = validarNuevoUsuario({ nombre: '', email: 'malo', password: '123', role: '' })
        expect(Object.keys(e).sort()).toEqual(['email', 'nombre', 'password', 'role'])
    })
})