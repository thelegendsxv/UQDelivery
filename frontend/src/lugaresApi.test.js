import { describe, expect, it, vi } from 'vitest'
import { buscarLugares, normalizarConsulta } from './lugaresApi.js'

const respuesta = (cuerpo, { ok = true, status = 200 } = {}) => ({ ok, status, json: async () => cuerpo })

describe('normalizarConsulta', () => {
    it('expande abreviaturas y quita el #', () => {
        expect(normalizarConsulta('Cra 14 # 5-15 barrio Granada')).toBe('Carrera 14 5-15 barrio Granada')
        expect(normalizarConsulta('cl 11d#23d-24')).toBe('Calle 11d 23d-24')
    })
    it('quita "No." pero no toca la palabra "no"', () => {
        expect(normalizarConsulta('Calle 5 No. 10-20')).toBe('Calle 5 10-20')
        expect(normalizarConsulta('Bar no te metas')).toBe('Bar no te metas')
    })
    it('colapsa espacios y tolera valores vacios', () => {
        expect(normalizarConsulta('  Granada   Armenia ')).toBe('Granada Armenia')
        expect(normalizarConsulta(undefined)).toBe('')
    })
})

describe('buscarLugares', () => {
    it('no consulta si el texto es muy corto', async () => {
        const fetchImpl = vi.fn()
        expect(await buscarLugares('ab', { fetchImpl })).toEqual([])
        expect(fetchImpl).not.toHaveBeenCalled()
    })

    it('llama al backend con la consulta normalizada y devuelve la lista', async () => {
        const lugares = [{ id: '1', titulo: 'Universidad del Quindío', subtitulo: 'La Fogata · Armenia', lat: 4.55, lng: -75.66 }]
        const fetchImpl = vi.fn().mockResolvedValue(respuesta(lugares))
        const r = await buscarLugares('Cra 14 # 5-15', { fetchImpl })
        expect(r).toEqual(lugares)
        const url = fetchImpl.mock.calls[0][0]
        expect(url.startsWith('/api/lugares?q=')).toBe(true)
        expect(decodeURIComponent(url.split('q=')[1].replace(/\+/g, ' '))).toBe('Carrera 14 5-15')
    })

    it('si la respuesta no es una lista devuelve []', async () => {
        const fetchImpl = vi.fn().mockResolvedValue(respuesta({ raro: true }))
        expect(await buscarLugares('Granada', { fetchImpl })).toEqual([])
    })

    it('lanza un error con el mensaje y el estado del servidor', async () => {
        const fetchImpl = vi.fn().mockResolvedValue(respuesta({ status: 503, message: 'Buscador no configurado' }, { ok: false, status: 503 }))
        await expect(buscarLugares('Granada', { fetchImpl })).rejects.toMatchObject({ message: 'Buscador no configurado', status: 503 })
    })
})
