package com.ulima.incidenciaurbana.service.impl;

import com.ulima.incidenciaurbana.dto.AsignacionDTO;
import com.ulima.incidenciaurbana.dto.ReporteDTO;
import com.ulima.incidenciaurbana.model.OperadorMunicipal;
import com.ulima.incidenciaurbana.model.Tecnico;
import com.ulima.incidenciaurbana.repository.OperadorMunicipalRepository;
import com.ulima.incidenciaurbana.repository.TecnicoRepository;
import com.ulima.incidenciaurbana.service.IAsignacionService;
import com.ulima.incidenciaurbana.service.IReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AsignacionServiceImpl implements IAsignacionService {

    private final OperadorMunicipalRepository operadorRepository;
    private final TecnicoRepository tecnicoRepository;
    private final IReporteService reporteService;

    @Autowired
    public AsignacionServiceImpl(OperadorMunicipalRepository operadorRepository,
            TecnicoRepository tecnicoRepository,
            IReporteService reporteService) {
        this.operadorRepository = operadorRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.reporteService = reporteService;
    }

    @Override
    public AsignacionDTO crearAsignacion(AsignacionDTO asignacionDTO) {
        OperadorMunicipal operador = operadorRepository.findById(asignacionDTO.getOperadorId())
                .orElseThrow(() -> new RuntimeException(
                        "Operador no encontrado con id: " + asignacionDTO.getOperadorId()));

        Tecnico tecnico = tecnicoRepository.findById(asignacionDTO.getTecnicoId())
                .orElseThrow(() -> new RuntimeException(
                        "Tecnico no encontrado con id: " + asignacionDTO.getTecnicoId()));

        ReporteDTO reporte = reporteService.asignarTecnico(asignacionDTO.getReporteId(), asignacionDTO.getTecnicoId());

        return new AsignacionDTO(
                null,
                reporte.getId(),
                asignacionDTO.getOperadorId(),
                asignacionDTO.getTecnicoId(),
                reporte.getTitulo(),
                operador.getPersona().getNombreCompleto(),
                tecnico.getPersona().getNombreCompleto(),
                LocalDateTime.now());
    }
}
