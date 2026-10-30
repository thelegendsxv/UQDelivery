import { describe, it, expect } from 'vitest'
import {
    aNumero, aSolicitudCotizacion, aSolicitudTarifa, COTIZACION_VACIA, validarCotizacion, validarTarifa,
} from './cotizacionValidation.js'

const VALIDA = {
    pesoKg: '2', largoCm: '30', anchoCm: '20', altoCm: '10',
    origenLat: '4.5339', origenLng: '-75.6811', destinoLat: '4.55', destinoLng: '-75.66',
    esFragil: true, esUrgente: false,
}

describe('aNumero', () => {
    it('acepta coma o punto y distingue vacio de invalido', () => {
        expect(aNumero('2,5')).toBe(2.5)
        expect(aNumero(' -75.68 ')).toBe(-75.68)
        expect(aNumero('')).toBeNull()
        expect(aNumero('abc')).toBeNaN()
        expect(aNumero('1e3')).toBeNaN()
    })
})

describe('validarCotizacion', () => {
    it('acepta una cotizacion completa', () => expect(validarCotizacion(VALIDA)).toEqual({}))
    it('reporta todos los datos pendientes con las claves del servidor', () =>
        expect(Object.keys(validarCotizacion(COTIZACION_VACIA)).sort()).toEqual(
            ['altoCm', 'anchoCm', 'destinoLat', 'destinoLng', 'largoCm', 'origenLat', 'origenLng', 'pesoKg']))
    it('rechaza medidas en cero, negativas o no numericas', () => {
        const e = validarCotizacion({ ...VALIDA, pesoKg: '0', largoCm: '-3', anchoCm: 'x' })
        expect(Object.keys(e).sort()).toEqual(['anchoCm', 'largoCm', 'pesoKg'])
    })
    it('valida el rango de las coordenadas', () => {
        const e = validarCotizacion({ ...VALIDA, origenLat: '90.1', destinoLng: '-180.5' })
        expect(Object.keys(e).sort()).toEqual(['destinoLng', 'origenLat'])
        expect(validarCotizacion({ ...VALIDA, origenLat: '-90', destinoLng: '180' })).toEqual({})
    })
})

describe('aSolicitudCotizacion', () => {
    it('convierte a numeros y las casillas a booleanos', () =>
        expect(aSolicitudCotizacion({ ...VALIDA, pesoKg: '2,5', esUrgente: undefined })).toEqual({
            pesoKg: 2.5, largoCm: 30, anchoCm: 20, altoCm: 10,
            origenLat: 4.5339, origenLng: -75.6811, destinoLat: 4.55, destinoLng: -75.66,
            esFragil: true, esUrgente: false,
        }))
})

describe('validarTarifa', () => {
    const TARIFA = {
        nombre: 'Urbana', tarifaBase: '5000', costoPorKm: '1200', costoPorKg: '800',
        recargoFragil: '3000', recargoUrgente: '5000', porcentajeSeguro: '1.5',
    }
    it('acepta una tarifa valida y la convierte', () => {
        expect(validarTarifa(TARIFA)).toEqual({})
        expect(aSolicitudTarifa({ ...TARIFA, nombre: ' Urbana ' })).toMatchObject({ nombre: 'Urbana', porcentajeSeguro: 1.5 })
    })
    it('rechaza negativos, mas de 2 decimales, html y porcentaje mayor a 100', () => {
        const e = validarTarifa({ ...TARIFA, nombre: '<b>', tarifaBase: '-1', costoPorKm: '1.234', porcentajeSeguro: '120' })
        expect(Object.keys(e).sort()).toEqual(['costoPorKm', 'nombre', 'porcentajeSeguro', 'tarifaBase'])
    })
})
