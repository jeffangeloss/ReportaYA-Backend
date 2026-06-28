package com.ulima.incidenciaurbana.service.impl;

import com.ulima.incidenciaurbana.dto.ReporteDTO;
import com.ulima.incidenciaurbana.model.EstadoReporte;
import com.ulima.incidenciaurbana.repository.OperadorMunicipalRepository;
import com.ulima.incidenciaurbana.service.IOperadorService;
import com.ulima.incidenciaurbana.service.IReporteQueryService;
import com.ulima.incidenciaurbana.service.IReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OperadorServiceImpl implements IOperadorService {

    private final OperadorMunicipalRepository operadorRepository;
    private final IReporteService reporteService;
    private final IReporteQueryService queryService;

    @Autowired
    public OperadorServiceImpl(OperadorMunicipalRepository operadorRepository,
            IReporteService reporteService,
            IReporteQueryService queryService) {
        this.operadorRepository = operadorRepository;
        this.reporteService = reporteService;
        this.queryService = queryService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReporteDTO> obtenerReportesPorEstado(String estado, int page) {
        if (estado == null || estado.trim().isEmpty()) {
            throw new IllegalArgumentException("El estado es requerido");
        }
        try {
            EstadoReporte estadoEnum = EstadoReporte.valueOf(estado.toUpperCase());
            return queryService.obtenerTodosReportes(Math.max(0, page), estadoEnum);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado invalido: " + estado);
        }
    }

    @Override
    public ReporteDTO aceptarReporte(Long reporteId, Long operadorId) {
        validarOperador(operadorId);
        return reporteService.cambiarEstadoReporte(reporteId, EstadoReporte.REVISION);
    }

    @Override
    public ReporteDTO rechazarReporte(Long reporteId, Long operadorId, String motivo) {
        validarOperador(operadorId);
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new RuntimeException("El motivo de rechazo es obligatorio");
        }
        return reporteService.rechazarReporte(reporteId, motivo);
    }

    private void validarOperador(Long operadorId) {
        if (operadorId == null) throw new IllegalArgumentException("operadorId es requerido");
        if (!operadorRepository.existsById(operadorId)) {
            throw new RuntimeException("Operador no encontrado con id: " + operadorId);
        }
    }
}
