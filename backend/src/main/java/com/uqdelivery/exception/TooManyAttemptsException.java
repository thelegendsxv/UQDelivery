package com.uqdelivery.exception;

public class TooManyAttemptsException extends RuntimeException {
    public TooManyAttemptsException() {
        super("Demasiados intentos fallidos. Intenta de nuevo en unos minutos");
    }
}
