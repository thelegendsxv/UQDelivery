package com.uqdelivery.exception;

/** HU-05 · No hay una tarifa activa con la cual cotizar. */
public class TarifaNoDisponibleException extends RuntimeException {
    public TarifaNoDisponibleException() {
        super("No hay una tarifa activa configurada. Intenta mas tarde o contacta al administrador");
    }
}
