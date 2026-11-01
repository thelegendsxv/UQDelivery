package com.uqdelivery.exception;

/** El proveedor de mapas no esta configurado o no respondio. Se traduce en 503. */
public class MapasNoDisponibleException extends RuntimeException {
    public MapasNoDisponibleException(String message) {
        super(message);
    }
}
