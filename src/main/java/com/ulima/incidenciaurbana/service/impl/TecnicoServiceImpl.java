package com.ulima.incidenciaurbana.service.impl;

import com.ulima.incidenciaurbana.dto.CompletarReporteRequest;
import com.ulima.incidenciaurbana.dto.ReporteDTO;
import com.ulima.incidenciaurbana.dto.TecnicoDTO;
import com.ulima.incidenciaurbana.model.*;
import com.ulima.incidenciaurbana.repository.ReporteRepository;
import com.ulima.incidenciaurbana.repository.TecnicoRepository;
import com.ulima.incidenciaurbana.service.IFotoService;
import com.ulima.incidenciaurbana.service.IReporteQueryService;
import com.ulima.incidenciaurbana.service.ITecnicoService;
import com.ulima.incidenciaurbana.service.IReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TecnicoServiceImpl implements ITecnicoService {

    private final ReporteRepository reporteRepository;
    private final IFotoService fotoService;
    private final IReporteService reporteService;
    private final IReporteQueryService queryService;
    private final TecnicoRepository tecnicoRepository;
    private static final int PAGE_SIZE = 10;

    @Autowired
    public TecnicoServiceImpl(ReporteRepository reporteRepository,
            IFotoService fotoService,
            IReporteService reporteService,
            IReporteQueryService queryService,
            TecnicoRepository tecnicoRepository) {
        this.reporteRepository = reporteRepository;
        this.fotoService = fotoService;
        this.reporteService = reporteService;
        this.queryService = queryService;
        this.tecnicoRepository = tecnicoRepository;
    }

    @Override
    public Page<TecnicoDTO> obtenerTodosTecnicos(int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("id").ascending());
        return tecnicoRepository.findAll(pageable).map(tecnico -> new TecnicoDTO(
                tecnico.getId(),
                tecnico.getUsuario(),
                tecnico.getPersona().getNombres(),
                tecnico.getPersona().getApellidos(),
                tecnico.getPersona().getCorreo()));
    }

    @Override
    public Page<ReporteDTO> obtenerReportesAsignados(Long tecnicoId, String estado, int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("id").descending());

        if (estado == null || estado.trim().isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0L);
        }

        try {
            EstadoReporte estadoEnum = EstadoReporte.valueOf(estado.toUpperCase());
            Page<Reporte> reportes = reporteRepository.findByTecnicoIdAndEstado(tecnicoId, estadoEnum, pageable);

            List<ReporteDTO> reportesDTO = reportes.getContent().stream()
                    .map(r -> queryService.obtenerReportePorId(r.getId()))
                    .collect(Collectors.toList());

            return new PageImpl<>(reportesDTO, pageable, reportes.getTotalElements());
        } catch (IllegalArgumentException e) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0L);
        }
    }

    @Override
    @Transactional
    public ReporteDTO completarReporte(Long tecnicoId, Long reporteId, CompletarReporteRequest request) {
        Objects.requireNonNull(reporteId, "reporteId no puede ser null");

        Reporte reporte = reporteRepository.findById(reporteId)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado"));

        if (reporte.getEstado() != EstadoReporte.REVISION) {
            throw new IllegalArgumentException(
                    "El reporte debe estar en estado REVISION para completarlo. Estado actual: " + reporte.getEstado());
        }

        if (reporte.getTecnico() == null || !Objects.equals(reporte.getTecnico().getId(), tecnicoId)) {
            throw new IllegalArgumentException("Solo el tecnico asignado puede completar el reporte");
        }

        for (CompletarReporteRequest.FotoRequestInline fotoRequest : request.getFotos()) {
            fotoService.subirFoto(reporteId, fotoRequest.getArchivoBase64(), TipoFoto.FINAL, fotoRequest.getDescripcion());
        }

        reporte.setComentarioResolucion(request.getComentarioResolucion());
        reporte.setFechaCierre(LocalDateTime.now());
        reporteRepository.save(reporte);

        reporteService.cambiarEstadoReporte(reporteId, EstadoReporte.FINALIZADO);

        return queryService.obtenerReportePorId(reporteId);
    }
}
