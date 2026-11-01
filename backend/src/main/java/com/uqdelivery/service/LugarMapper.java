package com.uqdelivery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.uqdelivery.dto.LugarResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Convierte la respuesta de TomTom en resultados cortos para la pantalla:
 * titulo = nombre del sitio o calle (con numero), subtitulo = calle del sitio (si aplica) · ciudad.
 * No se usa el barrio de TomTom (municipalitySubdivision): para Armenia devuelve "San Pedro" en casi todo y confunde.
 * Descarta el ruido administrativo ("Comuna Quimbaya", "Perimetro Urbano...", departamento, region).
 */
final class LugarMapper {

    private static final List<String> CAMPOS_SUBTITULO = List.of("municipality", "localName");
    private static final List<String> RUIDO = List.of("comuna", "perimetro", "corregimiento", "rap ");

    private LugarMapper() {
    }

    static List<LugarResponse> aLugares(JsonNode json) {
        return aLugares(json, null);
    }

    /** Con `ciudad` (p. ej. "Armenia") solo quedan los resultados de ese municipio; sin ella, todos. */
    static List<LugarResponse> aLugares(JsonNode json, String ciudad) {
        List<LugarResponse> lugares = new ArrayList<>();
        if (json == null) return lugares;
        Set<String> vistos = new HashSet<>();
        for (JsonNode r : json.path("results")) {
            JsonNode pos = r.path("position");
            if (!pos.path("lat").isNumber() || !pos.path("lon").isNumber()) continue;
            JsonNode dir = r.path("address");
            if (ciudad != null && !ciudad.isBlank() && !esDeCiudad(dir, ciudad)) continue;
            String titulo = titulo(r, dir);
            if (titulo.isBlank()) continue;
            String subtitulo = subtitulo(dir, titulo);
            if (!vistos.add(sinTildes(titulo + "|" + subtitulo))) continue;
            double lat = redondear(pos.path("lat").asDouble());
            double lng = redondear(pos.path("lon").asDouble());
            String id = texto(r.path("id"));
            lugares.add(new LugarResponse(id.isBlank() ? lat + "," + lng : id, titulo, subtitulo, lat, lng));
        }
        return lugares;
    }

    private static boolean esDeCiudad(JsonNode dir, String ciudad) {
        String buscada = sinTildes(ciudad.trim());
        return List.of("municipality", "localName", "countrySecondarySubdivision").stream()
                .anyMatch(campo -> sinTildes(texto(dir.path(campo))).equals(buscada));
    }

    private static String titulo(JsonNode r, JsonNode dir) {
        String sitio = texto(r.path("poi").path("name"));
        if (!sitio.isBlank()) return sitio;
        String calle = texto(dir.path("streetName"));
        if (!calle.isBlank()) {
            String numero = texto(dir.path("streetNumber"));
            return numero.isBlank() ? calle : calle + " " + numero;
        }
        String libre = texto(dir.path("freeformAddress"));
        int coma = libre.indexOf(',');
        return (coma > 0 ? libre.substring(0, coma) : libre).trim();
    }

    private static String subtitulo(JsonNode dir, String titulo) {
        String normalizadoTitulo = sinTildes(titulo);
        List<String> partes = new ArrayList<>();
        String calle = calle(dir);
        if (!calle.isBlank() && !sinTildes(calle).equals(normalizadoTitulo)) partes.add(calle); // sitios: su direccion
        for (String campo : CAMPOS_SUBTITULO) {
            String valor = texto(dir.path(campo));
            if (valor.isBlank()) continue;
            String normalizado = sinTildes(valor);
            if (normalizado.equals(normalizadoTitulo)) continue;
            if (RUIDO.stream().anyMatch(normalizado::startsWith)) continue;
            if (partes.stream().noneMatch(p -> sinTildes(p).equals(normalizado))) partes.add(valor);
        }
        return String.join(" · ", partes);
    }

    private static String calle(JsonNode dir) {
        String nombre = texto(dir.path("streetName"));
        if (nombre.isBlank()) return "";
        String numero = texto(dir.path("streetNumber"));
        return numero.isBlank() ? nombre : nombre + " " + numero;
    }

    private static String texto(JsonNode nodo) {
        return nodo == null || nodo.isMissingNode() || nodo.isNull() ? "" : nodo.asText("").trim();
    }

    private static String sinTildes(String t) {
        return Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static double redondear(double v) {
        return BigDecimal.valueOf(v).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }
}
