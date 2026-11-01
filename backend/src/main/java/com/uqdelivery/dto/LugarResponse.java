package com.uqdelivery.dto;

/** Un resultado de la busqueda de lugares: titulo (nombre o calle), subtitulo (barrio y ciudad) y coordenadas. */
public record LugarResponse(String id, String titulo, String subtitulo, double lat, double lng) {
}
