package com.ulima.incidenciaurbana.service;

import com.ulima.incidenciaurbana.dto.ReporteDTO;
import com.ulima.incidenciaurbana.model.EstadoReporte;

public interface IReporteService {

    ReporteDTO crearReporte(ReporteDTO reporteDTO);

    ReporteDTO actualizarReporte(Long id, ReporteDTO reporteDTO);

    ReporteDTO cambiarEstadoReporte(Long id, EstadoReporte nuevoEstado);

    ReporteDTO rechazarReporte(Long id, String motivo);

    void eliminarReporte(Long id);

    ReporteDTO asignarTecnico(Long reporteId, Long tecnicoId);
}
