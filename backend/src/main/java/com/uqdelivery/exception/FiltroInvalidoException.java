package com.uqdelivery.exception;

/** HU-14 · Filtros del historial incoherentes (por ejemplo, fecha inicial posterior a la final). Se responde con 400. */
public class FiltroInvalidoException extends RuntimeException {

    public FiltroInvalidoException(String message) { super(message); }
}
