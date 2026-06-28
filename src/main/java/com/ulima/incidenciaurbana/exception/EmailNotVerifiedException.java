package com.ulima.incidenciaurbana.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando las credenciales son correctas pero la cuenta aún no
 * verificó su correo (activo = false). Permite que el front distinga este
 * caso del de credenciales inválidas y ofrezca "reenviar verificación".
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Debes verificar tu correo antes de iniciar sesion");
    }
}
