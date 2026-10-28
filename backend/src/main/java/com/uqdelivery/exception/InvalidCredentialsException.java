package com.uqdelivery.exception;

/** Mensaje generico a proposito: no revela si fallo el correo o la contrasena. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() { super("Correo o contrasena incorrectos"); }
}
