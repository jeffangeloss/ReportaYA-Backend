package com.ulima.incidenciaurbana.service.impl;

import com.ulima.incidenciaurbana.dto.FotoDTO;
import com.ulima.incidenciaurbana.dto.ReporteDTO;
import com.ulima.incidenciaurbana.dto.ReporteMapaDTO;
import com.ulima.incidenciaurbana.dto.UbicacionDTO;
import com.ulima.incidenciaurbana.model.*;
import com.ulima.incidenciaurbana.repository.ReporteRepository;
import com.ulima.incidenciaurbana.repository.ReporteSpecification;
import com.ulima.incidenciaurbana.service.IReporteQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReporteQueryServiceImpl implements IReporteQueryService {

    private final ReporteRepository reporteRepository;
    private static final int PAGE_SIZE = 10;

    @Autowired
    public ReporteQueryServiceImpl(ReporteRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    @Override
    public ReporteDTO obtenerReportePorId(Long id) {
        if (id == null) throw new RuntimeException("El ID del reporte es obligatorio");
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado con id: " + id));
        return convertirADTO(reporte);
    }

    @Override
    public Page<ReporteDTO> obtenerTodosReportes(int page, EstadoReporte estado) {
        int p = Math.max(0, page);
        PageRequest pageRequest = PageRequest.of(p, PAGE_SIZE, Sort.by("fechaCreacion").descending());

        if (estado != null) {
            return reporteRepository.findByEstado(estado, pageRequest).map(this::convertirADTO);
        }
        return reporteRepository.findAll(pageRequest).map(this::convertirADTO);
    }

    @Override
    public Page<ReporteDTO> obtenerReportesPorCuenta(Long cuentaId, int page) {
        int p = Math.max(0, page);
        return reporteRepository
                .findByCuentaId(cuentaId, PageRequest.of(p, PAGE_SIZE, Sort.by("fechaCreacion").descending()))
                .map(this::convertirADTO);
    }

    @Override
    public List<ReporteMapaDTO> obtenerReportesMapa(EstadoReporte estado, TipoProblema tipo) {
        return reporteRepository.findAll(ReporteSpecification.filtrarParaMapa(estado, tipo)).stream()
                .map(this::convertirAMapaDTO)
                .collect(Collectors.toList());
    }

    private ReporteMapaDTO convertirAMapaDTO(Reporte reporte) {
        UbicacionDTO ubicacion = null;
        if (reporte.getUbicacion() != null) {
            ubicacion = new UbicacionDTO(
                    reporte.getUbicacion().getId(),
                    reporte.getUbicacion().getLatitud(),
                    reporte.getUbicacion().getLongitud(),
                    reporte.getUbicacion().getDireccion(),
                    reporte.getUbicacion().getFechaRegistro());
        }

        return new ReporteMapaDTO(
                reporte.getId(),
                reporte.getTitulo(),
                reporte.getEstado(),
                reporte.getTipoProblema(),
                ubicacion);
    }

    ReporteDTO convertirADTO(Reporte reporte) {
        ReporteDTO dto = new ReporteDTO();
        dto.setId(reporte.getId());
        dto.setTitulo(reporte.getTitulo());
        dto.setDescripcion(reporte.getDescripcion());
        dto.setCuentaId(reporte.getCuenta().getId());
        dto.setNombreCiudadano(reporte.getCuenta().getPersona().getNombreCompleto());
        dto.setEstado(reporte.getEstado());
        dto.setTipoProblema(reporte.getTipoProblema());
        dto.setFechaCreacion(reporte.getFechaCreacion());
        dto.setFechaActualizacion(reporte.getFechaActualizacion());
        dto.setComentarioResolucion(reporte.getComentarioResolucion());
        dto.setFechaCierre(reporte.getFechaCierre());

        if (reporte.getUbicacion() != null) {
            dto.setUbicacion(new UbicacionDTO(
                    reporte.getUbicacion().getId(),
                    reporte.getUbicacion().getLatitud(),
                    reporte.getUbicacion().getLongitud(),
                    reporte.getUbicacion().getDireccion(),
                    reporte.getUbicacion().getFechaRegistro()));
        }

        if (reporte.getFotos() != null && !reporte.getFotos().isEmpty()) {
            dto.setFotos(reporte.getFotos().stream()
                    .sorted((f1, f2) -> {
                        Map<TipoFoto, Integer> orden = Map.of(TipoFoto.INICIAL, 1, TipoFoto.FINAL, 2);
                        return orden.getOrDefault(f1.getTipo(), 99).compareTo(orden.getOrDefault(f2.getTipo(), 99));
                    })
                    .map(this::convertirFotoADTO)
                    .toList());
        }

        if (reporte.getTecnico() != null) {
            dto.setTecnicoAsignadoId(reporte.getTecnico().getId());
            dto.setTecnicoNombre(reporte.getTecnico().getPersona().getNombreCompleto());
        }

        return dto;
    }

    private FotoDTO convertirFotoADTO(Foto foto) {
        FotoDTO fotoDTO = new FotoDTO();
        fotoDTO.setId(foto.getId());
        fotoDTO.setReporteId(foto.getReporte().getId());
        fotoDTO.setUrl(foto.getUrl());
        fotoDTO.setTipo(foto.getTipo());
        fotoDTO.setDescripcion(foto.getDescripcion());
        fotoDTO.setFechaCarga(foto.getFechaCarga());
        return fotoDTO;
    }
}
