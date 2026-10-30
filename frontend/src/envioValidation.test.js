import { describe, it, expect } from 'vitest'
import { aSolicitudEnvio, validarDirecciones, validarReceptor } from './envioValidation.js'

describe('validarDirecciones', () => {
  it('acepta direcciones validas', () =>
    expect(validarDirecciones({ origenDireccion: 'Calle 1 # 2-3', destinoDireccion: 'Calle 9 # 10-11' })).toEqual({}))
  it('exige la direccion de origen y de destino', () => {
    const e = validarDirecciones({ origenDireccion: '', destinoDireccion: '' })
    expect(e.origenDireccion).toBeDefined()
    expect(e.destinoDireccion).toBeDefined()
  })
  it('rechaza html en la direccion', () =>
    expect(validarDirecciones({ origenDireccion: '<b>x</b>', destinoDireccion: 'Calle 9' }).origenDireccion).toBeDefined())
  it('rechaza una direccion demasiado larga', () =>
    expect(validarDirecciones({ origenDireccion: 'a'.repeat(256), destinoDireccion: 'Calle 9' }).origenDireccion).toBeDefined())
})

describe('validarReceptor', () => {
  const valido = { receptorNombre: 'Juan Perez', receptorTelefono: '3001234567', receptorEmail: 'juan@mail.com', descripcion: 'Libros' }

  it('acepta datos validos', () => expect(validarReceptor(valido)).toEqual({}))
  it('el correo y la descripcion son opcionales', () =>
    expect(validarReceptor({ ...valido, receptorEmail: '', descripcion: '' })).toEqual({}))
  it('exige nombre y telefono', () => {
    const e = validarReceptor({ receptorNombre: '', receptorTelefono: '' })
    expect(e.receptorNombre).toBeDefined()
    expect(e.receptorTelefono).toBeDefined()
  })
  it('rechaza un telefono con letras', () =>
    expect(validarReceptor({ ...valido, receptorTelefono: 'abc' }).receptorTelefono).toBeDefined())
  it('rechaza un correo con formato invalido', () =>
    expect(validarReceptor({ ...valido, receptorEmail: 'juan' }).receptorEmail).toBeDefined())
  it('rechaza html en el nombre y en la descripcion', () => {
    const e = validarReceptor({ ...valido, receptorNombre: '<script>x</script>', descripcion: '<img onerror=1>' })
    expect(e.receptorNombre).toBeDefined()
    expect(e.descripcion).toBeDefined()
  })
})

describe('aSolicitudEnvio', () => {
  const paquete = {
    pesoKg: 3, largoCm: 40, anchoCm: 30, altoCm: 20,
    origenDireccion: ' Calle 1 # 2-3 ', origenLat: 4.5339, origenLng: -75.6811,
    destinoDireccion: ' Calle 9 # 10-11 ', destinoLat: 4.545, destinoLng: -75.662,
    esFragil: true, esUrgente: false,
  }
  const receptor = { receptorNombre: ' Juan Perez ', receptorTelefono: ' 3001234567 ', receptorEmail: ' juan@mail.com ', descripcion: ' Libros ' }

  it('arma el cuerpo de la solicitud con los datos recortados', () => {
    const body = aSolicitudEnvio(paquete, receptor)
    expect(body).toEqual({
      pesoKg: 3, largoCm: 40, anchoCm: 30, altoCm: 20,
      origenDireccion: 'Calle 1 # 2-3', origenLat: 4.5339, origenLng: -75.6811,
      destinoDireccion: 'Calle 9 # 10-11', destinoLat: 4.545, destinoLng: -75.662,
      esFragil: true, esUrgente: false,
      receptorNombre: 'Juan Perez', receptorTelefono: '3001234567',
      receptorEmail: 'juan@mail.com', descripcion: 'Libros',
    })
  })

  it('el correo y la descripcion vacios quedan en null', () => {
    const body = aSolicitudEnvio(paquete, { ...receptor, receptorEmail: '', descripcion: '' })
    expect(body.receptorEmail).toBeNull()
    expect(body.descripcion).toBeNull()
  })
})
