import { describe, it, expect } from 'vitest'
import {
    aQuery, claseEstado, ESTADOS_ENVIO, etiquetaEstado, formatearFecha, formatearPrecio, hayFiltros, validarFiltros,
} from './historialValidation.js'

describe('validarFiltros', () => {
    it('acepta filtros vacios o solo con estado', () => {
        expect(validarFiltros({})).toEqual({})
        expect(validarFiltros({ estado: 'CANCELADO', desde: '', hasta: '' })).toEqual({})
    })
    it('acepta fechas en orden y el mismo dia como inicio y fin', () => {
        expect(validarFiltros({ desde: '2026-01-01', hasta: '2026-01-31' })).toEqual({})
        expect(validarFiltros({ desde: '2026-05-04', hasta: '2026-05-04' })).toEqual({})
    })
    it('rechaza una fecha final anterior a la inicial', () =>
        expect(validarFiltros({ desde: '2026-02-01', hasta: '2026-01-01' }).hasta).toBeDefined())
    it('rechaza fechas con formato invalido', () => {
        expect(validarFiltros({ desde: '01/02/2026' }).desde).toBeDefined()
        expect(validarFiltros({ hasta: 'ayer' }).hasta).toBeDefined()
    })
})

describe('aQuery / hayFiltros', () => {
    it('sin filtros no agrega nada a la URL', () => {
        expect(aQuery({})).toBe('')
        expect(aQuery({ estado: '', desde: '', hasta: '' })).toBe('')
        expect(hayFiltros({ estado: '', desde: '', hasta: '' })).toBe(false)
    })
    it('incluye solo los filtros con valor', () => {
        expect(aQuery({ estado: 'ENTREGADO' })).toBe('?estado=ENTREGADO')
        expect(aQuery({ estado: 'EN_RUTA', desde: '2026-01-01', hasta: '2026-01-31' }))
            .toBe('?estado=EN_RUTA&desde=2026-01-01&hasta=2026-01-31')
        expect(hayFiltros({ desde: '2026-01-01' })).toBe(true)
    })
})

describe('estados', () => {
    it('muestra un texto legible por cada estado del backend', () => {
        expect(ESTADOS_ENVIO.map((e) => e.value)).toEqual([
            'PENDIENTE_ASIGNACION', 'ASIGNADO', 'EN_RUTA', 'ENTREGADO', 'CANCELADO', 'CON_NOVEDAD',
        ])
        expect(etiquetaEstado('PENDIENTE_ASIGNACION')).toBe('Pendiente de asignación')
        expect(etiquetaEstado('CON_NOVEDAD')).toBe('Con novedad')
    })
    it('un estado desconocido se muestra tal cual', () => expect(etiquetaEstado('NUEVO')).toBe('NUEVO'))
    it('genera una clase por estado', () => {
        expect(claseEstado('EN_RUTA')).toBe('estado estado-en-ruta')
        expect(claseEstado('PENDIENTE_ASIGNACION')).toBe('estado estado-pendiente-asignacion')
    })
})

describe('formato', () => {
    it('formatea el precio en pesos con punto de miles', () => {
        expect(formatearPrecio(9564)).toBe('$ 9.564')
        expect(formatearPrecio('1234567.4')).toBe('$ 1.234.567')
        expect(formatearPrecio(0)).toBe('$ 0')
    })
    it('un precio vacio o invalido no muestra nada', () => {
        expect(formatearPrecio(null)).toBe('')
        expect(formatearPrecio('abc')).toBe('')
    })
    it('formatea la fecha dia/mes/año y tolera valores invalidos', () => {
        expect(formatearFecha('2026-11-02T15:30:00Z')).toMatch(/02\/11\/2026/)
        expect(formatearFecha('')).toBe('')
        expect(formatearFecha('no es fecha')).toBe('')
    })
})
