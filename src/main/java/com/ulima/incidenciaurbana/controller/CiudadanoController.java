package com.ulima.incidenciaurbana.controller;

import com.ulima.incidenciaurbana.dto.CiudadanoDTO;
import com.ulima.incidenciaurbana.service.ICiudadanoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ciudadanos")
public class CiudadanoController {

    private final ICiudadanoService ciudadanoService;

    @Autowired
    public CiudadanoController(ICiudadanoService ciudadanoService) {
        this.ciudadanoService = ciudadanoService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerCiudadano(@PathVariable Long id) {
        try {
            CiudadanoDTO ciudadano = ciudadanoService.obtenerCiudadanoPorId(id);
            return ResponseEntity.ok(ciudadano);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCiudadano(
            @PathVariable Long id,
            @RequestBody CiudadanoDTO ciudadanoDTO) {
        try {
            CiudadanoDTO actualizado = ciudadanoService.actualizarCiudadano(id, ciudadanoDTO);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCiudadano(@PathVariable Long id) {
        try {
            ciudadanoService.eliminarCiudadano(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
