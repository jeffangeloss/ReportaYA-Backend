package com.ulima.incidenciaurbana.controller;

import com.ulima.incidenciaurbana.dto.AsignacionDTO;
import com.ulima.incidenciaurbana.service.IAsignacionService;
import com.ulima.incidenciaurbana.util.RequestAuth;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/asignaciones")
public class AsignacionController {

    private final IAsignacionService asignacionService;

    @Autowired
    public AsignacionController(IAsignacionService asignacionService) {
        this.asignacionService = asignacionService;
    }

    @PostMapping
    public ResponseEntity<?> crearAsignacion(
            @Valid @RequestBody AsignacionDTO asignacionDTO,
            HttpServletRequest request) {
        // Solo un OPERADOR puede asignar; el operador se toma del token, no del body.
        RequestAuth.requireRole(request, RequestAuth.OPERADOR);
        asignacionDTO.setOperadorId(RequestAuth.cuentaId(request));
        try {
            AsignacionDTO asignacionCreada = asignacionService.crearAsignacion(asignacionDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(asignacionCreada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
