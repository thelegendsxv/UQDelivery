package com.uqdelivery.exception;

import com.uqdelivery.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "Datos invalidos", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, "Solicitud mal formada"));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(409, ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(401, ex.getMessage()));
    }

    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<ErrorResponse> handleTooMany(TooManyAttemptsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new ErrorResponse(429, ex.getMessage()));
    }

    //HU-02

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(409, ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "No tienes permiso para realizar esta accion"));
    }

    //HU-03

    @ExceptionHandler(ReseteoContrasenaException.class)
    public ResponseEntity<ErrorResponse> handlePasswordReset(ReseteoContrasenaException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(400, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Valor invalido para el parametro " + ex.getName()));
    }
}