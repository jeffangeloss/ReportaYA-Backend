package com.ulima.incidenciaurbana.service;

import com.ulima.incidenciaurbana.dto.ReporteDTO;
import com.ulima.incidenciaurbana.dto.ReporteMapaDTO;
import com.ulima.incidenciaurbana.model.EstadoReporte;
import com.ulima.incidenciaurbana.model.TipoProblema;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IReporteQueryService {

    ReporteDTO obtenerReportePorId(Long id);

    Page<ReporteDTO> obtenerTodosReportes(int page, EstadoReporte estado);

    Page<ReporteDTO> obtenerReportesPorCuenta(Long cuentaId, int page);

    List<ReporteMapaDTO> obtenerReportesMapa(EstadoReporte estado, TipoProblema tipo);
}
