package com.ulima.incidenciaurbana.controller;

import com.ulima.incidenciaurbana.dto.ReporteDTO;
import com.ulima.incidenciaurbana.service.IOperadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/operador")

public class OperadorController {

    private final IOperadorService operadorService;

    @Autowired
    public OperadorController(IOperadorService operadorService) {
        this.operadorService = operadorService;
    }

    @GetMapping("/reportes")
    public ResponseEntity<Page<ReporteDTO>> obtenerReportes(
            @RequestParam(name = "estado") String estado,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        try {
            Page<ReporteDTO> reportes = operadorService.obtenerReportesPorEstado(estado, page);
            return ResponseEntity.ok(reportes);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/reportes/{id}/aceptar")
    public ResponseEntity<?> aceptarReporte(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        try {
            ReporteDTO reporte = operadorService.aceptarReporte(id, body.get("operadorId"));
            return ResponseEntity.ok(reporte);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reportes/{id}/rechazar")
    public ResponseEntity<?> rechazarReporte(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            Long operadorId = ((Number) body.get("operadorId")).longValue();
            String motivo = (String) body.get("motivo");
            ReporteDTO reporte = operadorService.rechazarReporte(id, operadorId, motivo);
            return ResponseEntity.ok(reporte);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
