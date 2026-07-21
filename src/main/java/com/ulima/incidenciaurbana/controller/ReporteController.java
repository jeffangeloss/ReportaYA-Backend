package com.ulima.incidenciaurbana.controller;

import com.ulima.incidenciaurbana.dto.FotoDTO;
import com.ulima.incidenciaurbana.dto.ReporteDTO;
import com.ulima.incidenciaurbana.model.EstadoReporte;
import com.ulima.incidenciaurbana.model.TipoFoto;
import com.ulima.incidenciaurbana.model.TipoProblema;
import com.ulima.incidenciaurbana.service.IFotoService;
import com.ulima.incidenciaurbana.service.IReporteQueryService;
import com.ulima.incidenciaurbana.service.IReporteService;
import com.ulima.incidenciaurbana.util.RequestAuth;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final IReporteService reporteService;
    private final IReporteQueryService queryService;
    private final IFotoService fotoService;

    @Autowired
    public ReporteController(IReporteService reporteService, IReporteQueryService queryService,
            IFotoService fotoService) {
        this.reporteService = reporteService;
        this.queryService = queryService;
        this.fotoService = fotoService;
    }

    @PostMapping
    public ResponseEntity<?> crearReporte(@RequestBody ReporteDTO reporteDTO) {
        try {
            return new ResponseEntity<>(reporteService.crearReporte(reporteDTO), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> obtenerTodosReportes(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "estado", required = false) EstadoReporte estado) {
        return ResponseEntity.ok(queryService.obtenerTodosReportes(page, estado));
    }

    @GetMapping("/mapa")
    public ResponseEntity<?> obtenerReportesMapa(
            @RequestParam(name = "estado", required = false) EstadoReporte estado,
            @RequestParam(name = "tipo", required = false) TipoProblema tipo) {
        return ResponseEntity.ok(queryService.obtenerReportesMapa(estado, tipo));
    }

    @GetMapping("/cuenta/{cuentaId}")
    public ResponseEntity<?> obtenerReportesPorCuenta(
            @PathVariable Long cuentaId,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        return ResponseEntity.ok(queryService.obtenerReportesPorCuenta(cuentaId, page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerReportePorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(queryService.obtenerReportePorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarReporte(
            @PathVariable Long id, @RequestBody ReporteDTO reporteDTO,
            HttpServletRequest request) {
        RequestAuth.requireRole(request, RequestAuth.OPERADOR);
        try {
            return ResponseEntity.ok(reporteService.actualizarReporte(id, reporteDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Long id, @RequestParam EstadoReporte nuevoEstado,
            HttpServletRequest request) {
        RequestAuth.requireRole(request, RequestAuth.OPERADOR);
        try {
            return ResponseEntity.ok(reporteService.cambiarEstadoReporte(id, nuevoEstado));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazarReporte(
            @PathVariable Long id, @RequestParam String motivo,
            HttpServletRequest request) {
        RequestAuth.requireRole(request, RequestAuth.OPERADOR);
        try {
            return ResponseEntity.ok(reporteService.rechazarReporte(id, motivo));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/fotos")
    public ResponseEntity<?> subirFoto(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String archivoBase64 = body.get("archivoBase64");
            String tipoStr = body.getOrDefault("tipo", "INICIAL");
            String descripcion = body.getOrDefault("descripcion", "");

            if (archivoBase64 == null || archivoBase64.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "archivoBase64 es obligatorio"));
            }

            TipoFoto tipo = TipoFoto.valueOf(tipoStr.toUpperCase());
            FotoDTO foto = fotoService.subirFoto(id, archivoBase64, tipo, descripcion);
            return new ResponseEntity<>(foto, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tipo de foto invalido. Use INICIAL o FINAL"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/fotos")
    public ResponseEntity<?> obtenerFotos(
            @PathVariable Long id,
            @RequestParam(name = "tipo", required = false) String tipo) {
        try {
            ReporteDTO reporte = queryService.obtenerReportePorId(id);
            if (reporte.getFotos() == null) {
                return ResponseEntity.ok(List.of());
            }
            if (tipo != null && !tipo.trim().isEmpty()) {
                return ResponseEntity.ok(reporte.getFotos().stream()
                        .filter(f -> f.getTipo().name().equalsIgnoreCase(tipo.trim()))
                        .collect(Collectors.toList()));
            }
            return ResponseEntity.ok(reporte.getFotos());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarReporte(@PathVariable Long id, HttpServletRequest request) {
        RequestAuth.requireRole(request, RequestAuth.OPERADOR);
        try {
            reporteService.eliminarReporte(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
