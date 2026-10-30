package com.uqdelivery.exception;

/** HU-03 · Error de negocio en la recuperacion de contrasena. Se responde con 400. */
public class ReseteoContrasenaException extends RuntimeException {

    public ReseteoContrasenaException(String message) { super(message); }

    /** Mensaje unico para token inexistente, vencido o ya usado: no se revela cual de los tres fue. */
    public static ReseteoContrasenaException enlaceInvalido() {
        return new ReseteoContrasenaException("El enlace de recuperacion no es valido o ya expiro. Solicita uno nuevo");
    }
}
