package com.ulima.incidenciaurbana.service.impl;

import com.ulima.incidenciaurbana.dto.CuentaDTO;
import com.ulima.incidenciaurbana.model.*;
import com.ulima.incidenciaurbana.repository.CuentaRepository;
import com.ulima.incidenciaurbana.service.ICuentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CuentaServiceImpl implements ICuentaService {

    private final CuentaRepository cuentaRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public CuentaServiceImpl(CuentaRepository cuentaRepository, BCryptPasswordEncoder passwordEncoder) {
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public CuentaDTO crearCuenta(CuentaDTO dto) {
        Persona persona = new Persona(
                dto.getNombres(), dto.getApellidos(), dto.getDni(),
                dto.getTelefono(), dto.getCorreo());

        String hashedPassword = passwordEncoder.encode(dto.getContrasena());

        Cuenta cuenta = switch (dto.getTipoCuenta().toUpperCase()) {
            case "CIUDADANO" -> new Ciudadano(dto.getUsuario(), hashedPassword, persona);
            case "TECNICO" -> new Tecnico(dto.getUsuario(), hashedPassword, persona);
            case "OPERADOR_MUNICIPAL" -> new OperadorMunicipal(dto.getUsuario(), hashedPassword, persona);
            default -> throw new IllegalArgumentException(
                    "Tipo de cuenta no valido: " + dto.getTipoCuenta() +
                    ". Tipos validos: CIUDADANO, TECNICO, OPERADOR_MUNICIPAL");
        };

        cuenta.setActivo(dto.getActivo());
        Cuenta guardada = cuentaRepository.save(cuenta);

        CuentaDTO response = new CuentaDTO();
        response.setId(guardada.getId());
        response.setTipoCuenta(dto.getTipoCuenta());
        response.setUsuario(guardada.getUsuario());
        response.setNombres(guardada.getPersona().getNombres());
        response.setApellidos(guardada.getPersona().getApellidos());
        response.setDni(guardada.getPersona().getDni());
        response.setTelefono(guardada.getPersona().getTelefono());
        response.setCorreo(guardada.getPersona().getCorreo());
        response.setActivo(guardada.isActivo());
        return response;
    }
}
