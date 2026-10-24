package com.uqdelivery.domain;

/** Ciclo de vida de un envio. Compartido por todas las historias (HU-04, 06, 08, 11, 12, 13). */
public enum EstadoEnvio {
    PENDIENTE,
    ASIGNADO,
    EN_RUTA,
    ENTREGADO,
    CANCELADO
}
