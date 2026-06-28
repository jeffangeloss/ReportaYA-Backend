package com.ulima.incidenciaurbana.service.impl;

import com.ulima.incidenciaurbana.service.IEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Implementación de {@link IEmailService} usando la API HTTP de Resend.
 *
 * <p>Si {@code resend.api-key} está vacío, no falla: imprime el enlace de
 * verificación en consola (modo desarrollo), igual que el patrón de Firebase.
 */
@Service
public class ResendEmailServiceImpl implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailServiceImpl.class);
    private static final String RESEND_URL = "https://api.resend.com/emails";

    private final String apiKey;
    private final String from;
    private final String baseUrl;
    private final RestClient restClient;

    public ResendEmailServiceImpl(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from:onboarding@resend.dev}") String from,
            @Value("${app.base-url:http://localhost:8081}") String baseUrl) {
        this.apiKey = apiKey;
        this.from = from;
        this.baseUrl = baseUrl;

        // Timeouts para no retener el hilo/recursos si Resend tarda o no responde.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public void enviarVerificacion(String correo, String nombre, String token) {
        String enlace = baseUrl + "/api/cuenta/verificar?token=" + token;

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Resend] RESEND_API_KEY no configurada. Enlace de verificacion para {}: {}",
                    correo, enlace);
            return;
        }

        Map<String, Object> payload = Map.of(
                "from", from,
                "to", List.of(correo),
                "subject", "Verifica tu cuenta de ReportaYA",
                "html", construirHtml(nombre, enlace));

        try {
            restClient.post()
                    .uri(RESEND_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Resend] Correo de verificacion enviado a {}", correo);
        } catch (Exception e) {
            // No rompemos el registro si falla el envío: dejamos el enlace en logs.
            log.error("[Resend] Error enviando correo a {}: {}. Enlace: {}",
                    correo, e.getMessage(), enlace);
        }
    }

    private String construirHtml(String nombre, String enlace) {
        // Escapamos el nombre (dato del usuario) para evitar inyección de HTML en el correo.
        String saludo = (nombre != null && !nombre.isBlank()) ? HtmlUtils.htmlEscape(nombre) : "";
        return """
                <div style="font-family: system-ui, sans-serif; max-width:480px; margin:0 auto; padding:24px; color:#18181b;">
                  <h2 style="color:#7c3aed;">ReportaYA</h2>
                  <p>Hola %s, gracias por registrarte.</p>
                  <p>Para activar tu cuenta, confirma tu correo haciendo clic en el siguiente boton:</p>
                  <p style="text-align:center; margin:32px 0;">
                    <a href="%s" style="background:#7c3aed; color:#fff; padding:12px 28px; border-radius:8px; text-decoration:none; font-weight:600;">Verificar mi cuenta</a>
                  </p>
                  <p style="font-size:13px; color:#71717a;">Si el boton no funciona, copia y pega este enlace en tu navegador:<br>%s</p>
                  <p style="font-size:13px; color:#71717a;">Este enlace caduca en 24 horas. Si no creaste esta cuenta, ignora este correo.</p>
                </div>
                """.formatted(saludo, enlace, enlace);
    }
}
