package com.ulima.incidenciaurbana.util;

import com.ulima.incidenciaurbana.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Lee la identidad autenticada que el {@code JwtInterceptor} deja en el request
 * (cuentaId y tipoCuenta, tomados del JWT) y valida el rol. Con esto los
 * endpoints derivan al actor del TOKEN en vez de confiar en un id enviado por
 * el cliente en el body/path (cierra la escalada de privilegios / suplantacion).
 */
public final class RequestAuth {

    public static final String CIUDADANO = "CIUDADANO";
    public static final String TECNICO = "TECNICO";
    public static final String OPERADOR = "OPERADOR_MUNICIPAL";

    private RequestAuth() {}

    /** Id de la cuenta autenticada (del subject del JWT), o null. */
    public static Long cuentaId(HttpServletRequest req) {
        Object v = req.getAttribute("cuentaId");
        return (v instanceof Long) ? (Long) v : null;
    }

    /** Rol de la cuenta autenticada (CIUDADANO | TECNICO | OPERADOR_MUNICIPAL). */
    public static String tipoCuenta(HttpServletRequest req) {
        Object v = req.getAttribute("tipoCuenta");
        return v == null ? null : v.toString();
    }

    /** Exige que el usuario autenticado tenga uno de los roles indicados; si no, 403. */
    public static void requireRole(HttpServletRequest req, String... roles) {
        String actual = tipoCuenta(req);
        for (String r : roles) {
            if (r.equals(actual)) return;
        }
        throw new ForbiddenException("No tienes permiso para realizar esta accion.");
    }
}
