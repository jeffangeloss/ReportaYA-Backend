package com.ulima.incidenciaurbana.service.impl;

import com.ulima.incidenciaurbana.service.IEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Implementación de {@link IEmailService} usando la API HTTP de Resend.
 *
 * <p>Si {@code resend.api-key} está vacío, no falla: imprime el enlace
 * generado en consola (modo desarrollo), igual que el patrón de Firebase.
 */
@Service
public class ResendEmailServiceImpl implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailServiceImpl.class);
    private static final String RESEND_URL = "https://api.resend.com/emails";

    private final String apiKey;
    private final String from;
    private final String baseUrl;
    private final String passwordResetUrl;
    private final RestClient restClient;

    public ResendEmailServiceImpl(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from:onboarding@resend.dev}") String from,
            @Value("${app.base-url:http://localhost:8081}") String baseUrl,
            @Value("${app.password-reset-url:}") String passwordResetUrl) {
        this.apiKey = apiKey;
        this.from = from;
        this.baseUrl = baseUrl;
        this.passwordResetUrl = passwordResetUrl;

        // Timeouts para no retener el hilo/recursos si Resend tarda o no responde.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    @Async
    public void enviarVerificacion(String correo, String nombre, String token) {
        String enlace = baseUrl + "/api/cuenta/verificar?token=" + token;
        enviarCorreo(
                correo,
                "Verifica tu cuenta de ReportaYA",
                construirHtmlVerificacion(nombre, enlace),
                construirTextoVerificacion(nombre, enlace),
                enlace,
                "verificacion");
    }

    @Override
    @Async
    public void enviarRecuperacionContrasena(String correo, String nombre, String token) {
        // Opcion B: "token" es un codigo corto que el usuario ingresa en la app.
        // No se envia enlace (no hay deep link configurado); el codigo va en el correo.
        // Fallback de demo: dejamos el codigo en el log por si el correo no llega.
        log.info("[DEV] Codigo de recuperacion para {}: {}", correo, token);
        enviarCorreo(
                correo,
                "Tu codigo para restablecer la contrasena de ReportaYA",
                construirHtmlRecuperacion(nombre, token),
                construirTextoRecuperacion(nombre, token),
                token,
                "recuperacion de contrasena");
    }

    private void enviarCorreo(String correo, String subject, String html, String text, String enlace, String tipo) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[Resend] RESEND_API_KEY no configurada. Enlace de {} para {}: {}",
                    tipo, correo, enlace);
            return;
        }

        Map<String, Object> payload = Map.of(
                "from", from,
                "to", List.of(correo),
                "subject", subject,
                "html", html,
                "text", text);

        try {
            restClient.post()
                    .uri(RESEND_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Resend] Correo de {} enviado a {}", tipo, correo);
        } catch (Exception e) {
            log.error("[Resend] Error enviando correo de {} a {}: {}. Enlace: {}",
                    tipo, correo, e.getMessage(), enlace);
        }
    }

    private String construirEnlaceRecuperacion(String urlBase, String token, String correo) {
        String tokenSeguro = UriUtils.encodeQueryParam(token, StandardCharsets.UTF_8);
        String correoSeguro = UriUtils.encodeQueryParam(correo, StandardCharsets.UTF_8);
        boolean incluyeToken = urlBase.contains("{token}") || urlBase.contains("token=");
        boolean incluyeCorreo = urlBase.contains("{correo}") || urlBase.contains("correo=");
        if (urlBase.contains("{token}")) {
            urlBase = urlBase.replace("{token}", tokenSeguro);
        }
        if (urlBase.contains("{correo}")) {
            urlBase = urlBase.replace("{correo}", correoSeguro);
        }
        String separador = urlBase.contains("?") ? "&" : "?";
        String enlace = urlBase;
        if (!incluyeToken) {
            enlace += separador + "token=" + tokenSeguro;
            separador = "&";
        }
        if (!incluyeCorreo) {
            enlace += separador + "correo=" + correoSeguro;
        }
        return enlace;
    }

    private String construirHtmlVerificacion(String nombre, String enlace) {
        // Escapamos el nombre (dato del usuario) para evitar inyección de HTML en el correo.
        String saludo = (nombre != null && !nombre.isBlank()) ? HtmlUtils.htmlEscape(nombre) : "";
        String enlaceHtml = HtmlUtils.htmlEscape(enlace);
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
                """.formatted(saludo, enlaceHtml, enlaceHtml);
    }

    private String construirHtmlRecuperacion(String nombre, String codigo) {
        String saludo = (nombre != null && !nombre.isBlank()) ? HtmlUtils.htmlEscape(nombre) : "";
        String codigoHtml = HtmlUtils.htmlEscape(codigo);
        return """
                <div style="font-family: system-ui, sans-serif; max-width:480px; margin:0 auto; padding:24px; color:#18181b;">
                  <h2 style="color:#7c3aed;">ReportaYA</h2>
                  <p>Hola %s, recibimos una solicitud para restablecer tu contraseña.</p>
                  <p>Ingresa este código en la app para crear tu nueva contraseña:</p>
                  <p style="text-align:center; margin:32px 0;">
                    <span style="display:inline-block; background:#f4f4f5; color:#7c3aed; font-size:32px; font-weight:700; letter-spacing:8px; padding:16px 28px; border-radius:12px; font-family:monospace;">%s</span>
                  </p>
                  <p style="font-size:13px; color:#71717a;">Este código caduca en 30 minutos. Si no solicitaste este cambio, ignora este correo.</p>
                </div>
                """.formatted(saludo, codigoHtml);
    }

    private String construirTextoVerificacion(String nombre, String enlace) {
        String saludo = construirSaludoTexto(nombre);
        return """
                Hola%s,

                Para completar tu registro en ReportaYA, verifica tu cuenta aqui:
                %s

                Este enlace caduca en 24 horas. Si no creaste esta cuenta, ignora este correo.

                Equipo de ReportaYA
                """.formatted(saludo, enlace);
    }

    private String construirTextoRecuperacion(String nombre, String codigo) {
        String saludo = construirSaludoTexto(nombre);
        return """
                Hola%s,

                Recibimos una solicitud para restablecer tu contrasena de ReportaYA.
                Ingresa este codigo en la app para crear tu nueva contrasena:

                %s

                Este codigo caduca en 30 minutos. Si no solicitaste este cambio, ignora este correo.

                Equipo de ReportaYA
                """.formatted(saludo, codigo);
    }

    private String construirSaludoTexto(String nombre) {
        return (nombre != null && !nombre.isBlank()) ? " " + nombre.trim() : "";
    }
}
