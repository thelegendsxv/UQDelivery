package com.uqdelivery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uqdelivery.dto.LugarResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LugarMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static List<LugarResponse> mapear(String json) throws Exception {
        JsonNode nodo = MAPPER.readTree(json);
        return LugarMapper.aLugares(nodo);
    }

    @Test
    void unSitioMuestraSuNombreSuCalleYLaCiudadSinElBarrioDelProveedor() throws Exception {
        List<LugarResponse> r = mapear("""
                {"results":[{"id":"abc","poi":{"name":"Universidad del Quindío"},
                 "address":{"streetName":"Carrera 15","streetNumber":"12N","municipalitySubdivision":"San Pedro",
                            "municipality":"Armenia","localName":"Armenia",
                            "freeformAddress":"Carrera 15, Armenia"},
                 "position":{"lat":4.5533,"lon":-75.6606}}]}""");
        assertEquals(1, r.size());
        assertEquals("Universidad del Quindío", r.get(0).titulo());
        assertEquals("Carrera 15 12N · Armenia", r.get(0).subtitulo());
        assertEquals(4.5533, r.get(0).lat());
        assertEquals(-75.6606, r.get(0).lng());
    }

    @Test
    void unaCalleMuestraNombreConNumero() throws Exception {
        List<LugarResponse> r = mapear("""
                {"results":[{"address":{"streetName":"Calle 11","streetNumber":"23D","municipalitySubdivision":"Granada",
                 "municipality":"Armenia"},"position":{"lat":4.54,"lon":-75.67}}]}""");
        assertEquals("Calle 11 23D", r.get(0).titulo());
        assertEquals("Armenia", r.get(0).subtitulo());
    }

    @Test
    void sinNombreNiCalleUsaElPrimerTramoDeLaDireccionLibre() throws Exception {
        List<LugarResponse> r = mapear("""
                {"results":[{"address":{"freeformAddress":"Granada, Armenia","municipality":"Armenia"},
                 "position":{"lat":4.54,"lon":-75.67}}]}""");
        assertEquals("Granada", r.get(0).titulo());
        assertEquals("Armenia", r.get(0).subtitulo());
    }

    @Test
    void descartaElRuidoAdministrativoYLosRepetidos() throws Exception {
        List<LugarResponse> r = mapear("""
                {"results":[
                 {"poi":{"name":"Parque Uribe"},"address":{"municipalitySubdivision":"Comuna Fundadores",
                  "municipality":"Armenia","localName":"Armenia"},"position":{"lat":4.5,"lon":-75.6}},
                 {"poi":{"name":"parque uribe"},"address":{"municipality":"Armenia"},"position":{"lat":4.5,"lon":-75.6}}]}""");
        assertEquals(1, r.size());
        assertEquals("Armenia", r.get(0).subtitulo());
    }

    @Test
    void ignoraResultadosSinCoordenadasYRespuestasVacias() throws Exception {
        assertTrue(mapear("{\"results\":[{\"poi\":{\"name\":\"Sin posicion\"}}]}").isEmpty());
        assertTrue(mapear("{}").isEmpty());
        assertTrue(LugarMapper.aLugares(null).isEmpty());
    }

    @Test
    void conCiudadDejaSoloLosResultadosDeEsaCiudad() throws Exception {
        JsonNode nodo = MAPPER.readTree("""
                {"results":[
                 {"poi":{"name":"Universidad del Quindío"},"address":{"municipality":"Armenia"},"position":{"lat":4.55,"lon":-75.66}},
                 {"poi":{"name":"Universidad del Quindio"},"address":{"municipality":"Pereira","municipalitySubdivision":"Boston"},
                  "position":{"lat":4.8,"lon":-75.7}}]}""");
        List<LugarResponse> r = LugarMapper.aLugares(nodo, "Armenia");
        assertEquals(1, r.size());
        assertEquals("Armenia", r.get(0).subtitulo());
        assertEquals(2, LugarMapper.aLugares(nodo).size());
    }
}
