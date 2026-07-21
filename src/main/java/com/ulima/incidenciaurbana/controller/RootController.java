package com.ulima.incidenciaurbana.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint informativo en la raiz "/". Devuelve el nombre de la API, su estado
 * y los modulos disponibles, para que abrir la URL del backend en el navegador
 * muestre algo util en vez de la pagina de error por defecto de Spring.
 */
@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", "ReportaYA Backend");
        info.put("status", "running");
        info.put("health", "/health");
        info.put("modules", List.of(
                "/api/auth",
                "/api/cuenta",
                "/api/reportes",
                "/api/reportes/{id}/fotos",
                "/api/operador",
                "/api/asignaciones",
                "/api/tecnicos",
                "/api/ciudadanos",
                "/api/historial-estados",
                "/api/notificaciones"
        ));
        return ResponseEntity.ok(info);
    }
}
