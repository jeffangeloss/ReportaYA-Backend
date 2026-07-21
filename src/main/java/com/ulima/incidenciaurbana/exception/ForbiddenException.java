package com.ulima.incidenciaurbana.exception;

/**
 * Se lanza cuando el usuario autenticado no tiene el rol/permiso necesario para
 * la accion solicitada. El GlobalExceptionHandler la traduce a HTTP 403.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
