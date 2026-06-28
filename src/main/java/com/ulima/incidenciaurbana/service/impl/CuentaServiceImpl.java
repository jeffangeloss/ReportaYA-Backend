package com.ulima.incidenciaurbana.service.impl;

import com.ulima.incidenciaurbana.dto.CuentaDTO;
import com.ulima.incidenciaurbana.model.*;
import com.ulima.incidenciaurbana.repository.CuentaRepository;
import com.ulima.incidenciaurbana.service.ICuentaService;
import com.ulima.incidenciaurbana.service.IEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class CuentaServiceImpl implements ICuentaService {

    /** Horas de validez del enlace de verificación de correo. */
    private static final int HORAS_VALIDEZ_TOKEN = 24;

    private final CuentaRepository cuentaRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final IEmailService emailService;

    @Autowired
    public CuentaServiceImpl(CuentaRepository cuentaRepository,
                             BCryptPasswordEncoder passwordEncoder,
                             IEmailService emailService) {
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    public CuentaDTO crearCuenta(CuentaDTO dto) {
        // El registro publico SOLO crea CIUDADANO. Operadores y tecnicos se crean
        // por SQL / canal administrativo: evita escalada de privilegios desde un
        // endpoint anonimo enviando tipoCuenta = OPERADOR_MUNICIPAL / TECNICO.
        String tipo = (dto.getTipoCuenta() == null) ? "" : dto.getTipoCuenta().toUpperCase();
        if (!"CIUDADANO".equals(tipo)) {
            throw new IllegalArgumentException(
                    "El registro publico solo permite crear cuentas de tipo CIUDADANO");
        }

        Persona persona = new Persona(
                dto.getNombres(), dto.getApellidos(), dto.getDni(),
                dto.getTelefono(), dto.getCorreo());

        String hashedPassword = passwordEncoder.encode(dto.getContrasena());
        Cuenta cuenta = new Ciudadano(dto.getUsuario(), hashedPassword, persona);

        // La cuenta nace INACTIVA hasta que el usuario verifique su correo.
        cuenta.setActivo(false);
        String token = UUID.randomUUID().toString();
        cuenta.asignarTokenVerificacion(token, LocalDateTime.now().plusHours(HORAS_VALIDEZ_TOKEN));

        Cuenta guardada = cuentaRepository.save(cuenta);

        // Envía el correo de verificación (no rompe el registro si el envío falla).
        emailService.enviarVerificacion(
                guardada.getPersona().getCorreo(),
                guardada.getPersona().getNombres(),
                token);

        CuentaDTO response = new CuentaDTO();
        response.setId(guardada.getId());
        response.setTipoCuenta(guardada.getTipoCuenta());
        response.setUsuario(guardada.getUsuario());
        response.setNombres(guardada.getPersona().getNombres());
        response.setApellidos(guardada.getPersona().getApellidos());
        response.setDni(guardada.getPersona().getDni());
        response.setTelefono(guardada.getPersona().getTelefono());
        response.setCorreo(guardada.getPersona().getCorreo());
        response.setActivo(guardada.isActivo());
        return response;
    }

    @Override
    public void verificarCuenta(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token no proporcionado");
        }

        Cuenta cuenta = cuentaRepository.findByTokenVerificacion(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Enlace de verificacion invalido o ya utilizado"));

        if (cuenta.tokenVerificacionExpirado()) {
            throw new IllegalArgumentException(
                    "El enlace de verificacion ha expirado. Solicita uno nuevo.");
        }

        cuenta.confirmarVerificacion();
        cuentaRepository.save(cuenta);
    }

    @Override
    public void reenviarVerificacion(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }

        // Respuesta uniforme para no revelar qué correos existen ni su estado
        // (anti-enumeración): si no existe o ya está verificada, no hacemos nada.
        cuentaRepository.findByCorreo(correo.trim()).ifPresent(cuenta -> {
            if (cuenta.isActivo()) {
                return;
            }
            String token = UUID.randomUUID().toString();
            cuenta.asignarTokenVerificacion(token, LocalDateTime.now().plusHours(HORAS_VALIDEZ_TOKEN));
            cuentaRepository.save(cuenta);
            emailService.enviarVerificacion(
                    cuenta.getPersona().getCorreo(),
                    cuenta.getPersona().getNombres(),
                    token);
        });
    }
}
