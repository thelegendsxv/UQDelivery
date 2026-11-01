package com.uqdelivery.service;

import com.uqdelivery.dto.LugarResponse;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Puntos de entrega que el equipo define a mano porque los mapas no los tienen bien (o no los tienen):
 * por ejemplo, las entradas de la Universidad del Quindio. Si lo que escribe el usuario coincide con alguno,
 * se devuelven SOLO estos puntos y no se consulta a TomTom.
 *
 * Para agregar uno: copiar sus coordenadas en Google Maps (clic derecho sobre el sitio -> clic en los numeros).
 */
public final class PuntosConocidos {

    private record Punto(LugarResponse lugar, List<String> palabras) {
    }

    private static final List<String> UNIVERSIDAD = List.of("universidad del quindio", "universidad quindio", "uniquindio");

    // >>> ENTRADA CIENCIAS BASICAS: pegar aqui sus coordenadas (Google Maps: clic derecho sobre la entrada -> clic en los numeros).
    // Mientras esten en 0 la entrada no se muestra (para no enviar al repartidor a un punto falso).
    private static final double BASICAS_LAT = 4.552728;
    private static final double BASICAS_LNG = -75.659611;

    private static final List<Punto> PUNTOS = Stream.of(
            new Punto(new LugarResponse("uq-ingenieria", "Universidad del Quindío · Entrada Ingeniería", "Armenia",
                    4.553868, -75.661002), UNIVERSIDAD),
            new Punto(new LugarResponse("uq-basicas", "Universidad del Quindío · Entrada Ciencias Básicas", "Armenia",
                    BASICAS_LAT, BASICAS_LNG), UNIVERSIDAD)
    ).filter(p -> p.lugar().lat() != 0 || p.lugar().lng() != 0).toList();

    private PuntosConocidos() {
    }

    /** Puntos que coinciden con el texto (vacio si ninguno). Tolera tildes, mayusculas y texto a medio escribir. */
    public static List<LugarResponse> buscar(String texto) {
        String n = sinTildes(texto).replaceAll("\\s+", " ").trim();
        if (n.length() < 4) return List.of();
        return PUNTOS.stream()
                .filter(p -> p.palabras().stream().anyMatch(k -> n.contains(k) || k.startsWith(n)))
                .map(Punto::lugar)
                .toList();
    }

    private static String sinTildes(String t) {
        return Normalizer.normalize(String.valueOf(t), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
