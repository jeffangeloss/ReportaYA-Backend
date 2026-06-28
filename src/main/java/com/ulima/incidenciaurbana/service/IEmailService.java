package com.ulima.incidenciaurbana.service;

/**
 * Servicio de envío de correos (verificación de cuenta).
 */
public interface IEmailService {

    /**
     * Envía el correo con el enlace de verificación de cuenta.
     *
     * @param correo correo destino
     * @param nombre nombre del destinatario (para personalizar el mensaje)
     * @param token  token de verificación que viajará en el enlace
     */
    void enviarVerificacion(String correo, String nombre, String token);
}
