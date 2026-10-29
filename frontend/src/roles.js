export const ROLES = {
    ADMINISTRADOR: 'ADMINISTRADOR',
    CLIENTE_EMISOR: 'CLIENTE_EMISOR',
    REPARTIDOR: 'REPARTIDOR',
}

export const ROLE_LABELS = {
    ADMINISTRADOR: 'Administrador',
    CLIENTE_EMISOR: 'Cliente emisor',
    REPARTIDOR: 'Repartidor',
}

export const ROLE_OPTIONS = Object.values(ROLES).map((value) => ({ value, label: ROLE_LABELS[value] }))