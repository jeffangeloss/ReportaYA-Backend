package com.ulima.incidenciaurbana.service;

import com.ulima.incidenciaurbana.dto.CuentaDTO;

/**
 * Interfaz del servicio unificado para creación de cuentas
 * Usa Abstract Factory para crear diferentes tipos de cuentas
 */
public interface ICuentaService {
    /**
     * Crea una cuenta de cualquier tipo usando Abstract Factory
     * @param cuentaDTO datos de la cuenta a crear
     * @return datos de la cuenta creada
     * @throws IllegalArgumentException si el tipo de cuenta no es válido
     * @throws RuntimeException si hay errores de duplicación o validación
     */
    CuentaDTO crearCuenta(CuentaDTO cuentaDTO);

    /**
     * Verifica una cuenta a partir del token enviado por correo y la activa.
     * @param token token de verificación recibido por correo
     * @throws IllegalArgumentException si el token es inválido o expiró
     */
    void verificarCuenta(String token);

    /**
     * Reenvía el correo de verificación a una cuenta aún no activada.
     * @param correo correo de la cuenta
     */
    void reenviarVerificacion(String correo);
}
