// sessionStorage: la sesion se borra al cerrar la pestana (menos exposicion que localStorage).
const KEY = 'uqdelivery.session'

export function saveSession(session) {
  sessionStorage.setItem(KEY, JSON.stringify(session))
}

export function getSession() {
  try {
    return JSON.parse(sessionStorage.getItem(KEY))
  } catch {
    return null
  }
}

export function clearSession() {
  sessionStorage.removeItem(KEY)
}
