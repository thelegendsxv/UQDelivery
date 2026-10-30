package com.uqdelivery.exception;

public class TooManyAttemptsException extends RuntimeException {
    public TooManyAttemptsException() {
        super("Demasiados intentos fallidos. Intenta de nuevo en unos minutos");
    }

    /** HU-03: permite un mensaje propio (p. ej. demasiadas solicitudes de recuperacion). */
    public TooManyAttemptsException(String message) {
        super(message);
    }
}
