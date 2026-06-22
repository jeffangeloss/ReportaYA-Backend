package com.ulima.incidenciaurbana.service.impl;

import com.ulima.incidenciaurbana.dto.AsignacionDTO;
import com.ulima.incidenciaurbana.dto.ReporteDTO;
import com.ulima.incidenciaurbana.model.*;
import com.ulima.incidenciaurbana.repository.CuentaRepository;
import com.ulima.incidenciaurbana.service.IAsignacionService;
import com.ulima.incidenciaurbana.service.IReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AsignacionServiceImpl implements IAsignacionService {

    private final CuentaRepository cuentaRepository;
    private final IReporteService reporteService;

    @Autowired
    public AsignacionServiceImpl(CuentaRepository cuentaRepository, IReporteService reporteService) {
        this.cuentaRepository = cuentaRepository;
        this.reporteService = reporteService;
    }

    @Override
    public AsignacionDTO crearAsignacion(AsignacionDTO asignacionDTO) {
        Cuenta operadorCuenta = cuentaRepository.findById(asignacionDTO.getOperadorId())
                .orElseThrow(() -> new RuntimeException(
                        "Operador no encontrado con id: " + asignacionDTO.getOperadorId()));

        if (!(operadorCuenta instanceof OperadorMunicipal)) {
            throw new RuntimeException(
                    "El usuario con id " + asignacionDTO.getOperadorId() + " no es un operador municipal");
        }

        Cuenta tecnicoCuenta = cuentaRepository.findById(asignacionDTO.getTecnicoId())
                .orElseThrow(() -> new RuntimeException(
                        "Tecnico no encontrado con id: " + asignacionDTO.getTecnicoId()));

        if (!(tecnicoCuenta instanceof Tecnico)) {
            throw new RuntimeException(
                    "El usuario con id " + asignacionDTO.getTecnicoId() + " no es un tecnico");
        }

        ReporteDTO reporte = reporteService.asignarTecnico(asignacionDTO.getReporteId(), asignacionDTO.getTecnicoId());

        return new AsignacionDTO(
                null,
                reporte.getId(),
                asignacionDTO.getOperadorId(),
                asignacionDTO.getTecnicoId(),
                reporte.getTitulo(),
                operadorCuenta.getPersona().getNombreCompleto(),
                tecnicoCuenta.getPersona().getNombreCompleto(),
                java.time.LocalDateTime.now());
    }
}
