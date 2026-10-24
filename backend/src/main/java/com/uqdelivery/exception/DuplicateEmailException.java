package com.uqdelivery.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() { super("Ya existe una cuenta registrada con ese correo"); }
}
