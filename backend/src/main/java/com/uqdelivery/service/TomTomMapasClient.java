package com.uqdelivery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.uqdelivery.dto.LugarResponse;
import com.uqdelivery.exception.MapasNoDisponibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;

/**
 * HU-04 y HU-05 · Unico lugar que conoce a TomTom: busqueda de lugares (Search API) y distancia por calles
 * (Routing API). La clave viene de la variable de entorno TOMTOM_API_KEY y nunca sale del servidor.
 * Para cambiar de proveedor (Google, HERE, ...) basta reemplazar esta clase.
 */
@Component
public class TomTomMapasClient {

    private static final Logger log = LoggerFactory.getLogger(TomTomMapasClient.class);
    private static final String BASE = "https://api.tomtom.com";

    private final RestClient http;
    private final String clave;
    private final double centroLat;
    private final double centroLng;
    private final int radioMetros;
    private final String ciudad;

    public TomTomMapasClient(@Value("${app.maps.tomtom-key:}") String clave,
                             @Value("${app.maps.centro-lat:4.5339}") double centroLat,
                             @Value("${app.maps.centro-lng:-75.6811}") double centroLng,
                             @Value("${app.maps.radio-metros:10000}") int radioMetros,
                             @Value("${app.maps.ciudad:Armenia}") String ciudad) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(3000);
        fabrica.setReadTimeout(5000);
        this.http = RestClient.builder().requestFactory(fabrica).build();
        this.clave = clave == null ? "" : clave.trim();
        this.centroLat = centroLat;
        this.centroLng = centroLng;
        this.radioMetros = radioMetros;
        this.ciudad = ciudad == null ? "" : ciudad.trim();
    }

    public boolean disponible() {
        return !clave.isBlank();
    }

    /** Lugares que coinciden con el texto, solo de la ciudad configurada (app.maps.ciudad) y dentro del radio urbano. */
    public List<LugarResponse> buscar(String texto) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(BASE)
                .pathSegment("search", "2", "search", texto + ".json")
                .queryParam("key", clave)
                .queryParam("countrySet", "CO")
                .queryParam("lat", centroLat)
                .queryParam("lon", centroLng)
                .queryParam("limit", 12)
                .queryParam("typeahead", true)
                .queryParam("language", "es-ES");
        if (radioMetros > 0) b.queryParam("radius", radioMetros);
        URI uri = b.build().encode().toUri();
        try {
            List<LugarResponse> todos = LugarMapper.aLugares(http.get().uri(uri).retrieve().body(JsonNode.class), ciudad);
            return todos.size() > 6 ? todos.subList(0, 6) : todos;
        } catch (RestClientException e) {
            // No se registra e.getMessage(): en errores de red incluye la URL, y la URL lleva la clave.
            log.warn("TomTom (busqueda) fallo: {}", resumen(e));
            throw new MapasNoDisponibleException("No pudimos consultar el mapa en este momento. Intenta de nuevo en unos segundos.");
        }
    }

    /** Distancia en metros por las calles (en carro) entre dos puntos. Lanza RestClientException si TomTom falla. */
    public OptionalInt distanciaMetros(double origenLat, double origenLng, double destinoLat, double destinoLng) {
        String ruta = String.format(Locale.ROOT, "%.6f,%.6f:%.6f,%.6f", origenLat, origenLng, destinoLat, destinoLng);
        URI uri = UriComponentsBuilder.fromUriString(BASE)
                .pathSegment("routing", "1", "calculateRoute", ruta, "json")
                .queryParam("key", clave)
                .queryParam("travelMode", "car")
                .queryParam("traffic", false)
                .build().encode().toUri();
        JsonNode json = http.get().uri(uri).retrieve().body(JsonNode.class);
        JsonNode metros = json == null ? null : json.path("routes").path(0).path("summary").path("lengthInMeters");
        return metros != null && metros.isNumber() ? OptionalInt.of(metros.asInt()) : OptionalInt.empty();
    }

    static String resumen(RestClientException e) {
        return e instanceof RestClientResponseException r ? "HTTP " + r.getStatusCode().value() : e.getClass().getSimpleName();
    }
}
