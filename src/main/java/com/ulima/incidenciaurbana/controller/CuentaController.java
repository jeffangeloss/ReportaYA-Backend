package com.ulima.incidenciaurbana.controller;

import com.ulima.incidenciaurbana.dto.CuentaDTO;
import com.ulima.incidenciaurbana.service.ICuentaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cuenta")
public class CuentaController {

    private final ICuentaService cuentaService;

    @Autowired
    public CuentaController(ICuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @PostMapping
    public ResponseEntity<?> crearCuenta(@Valid @RequestBody CuentaDTO cuentaDTO) {
        try {
            CuentaDTO cuentaCreada = cuentaService.crearCuenta(cuentaDTO);
            return new ResponseEntity<>(cuentaCreada, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Enlace al que llega el usuario desde el correo. Activa la cuenta y
     * devuelve una página HTML simple de confirmación.
     */
    @GetMapping("/verificar")
    public ResponseEntity<String> verificar(@RequestParam("token") String token) {
        try {
            cuentaService.verificarCuenta(token);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(paginaHtml("Cuenta verificada",
                            "Tu cuenta fue verificada con exito. Ya puedes iniciar sesion en ReportaYA.",
                            true));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_HTML)
                    .body(paginaHtml("No se pudo verificar", e.getMessage(), false));
        }
    }

    @PostMapping("/reenviar-verificacion")
    public ResponseEntity<?> reenviarVerificacion(@RequestBody Map<String, String> body) {
        try {
            cuentaService.reenviarVerificacion(body.get("correo"));
            return ResponseEntity.ok(Map.of(
                    "message", "Si el correo existe y aun no esta verificado, te reenviamos el enlace."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String paginaHtml(String titulo, String mensaje, boolean exito) {
        String color = exito ? "#16a34a" : "#dc2626";
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="font-family: system-ui, sans-serif; background:#f4f4f5; margin:0; padding:0;">
                  <div style="max-width:480px; margin:80px auto; background:#fff; border-radius:12px; padding:40px; text-align:center; box-shadow:0 4px 16px rgba(0,0,0,0.08);">
                    <h1 style="color:%s; margin-bottom:12px;">%s</h1>
                    <p style="color:#3f3f46; font-size:16px; line-height:1.5;">%s</p>
                  </div>
                </body>
                </html>
                """.formatted(titulo, color, titulo, mensaje);
    }
}
