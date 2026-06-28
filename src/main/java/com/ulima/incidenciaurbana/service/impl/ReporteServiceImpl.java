package com.ulima.incidenciaurbana.service.impl;

import com.ulima.incidenciaurbana.dto.ReporteDTO;
import com.ulima.incidenciaurbana.model.*;
import com.ulima.incidenciaurbana.repository.CuentaRepository;
import com.ulima.incidenciaurbana.repository.ReporteRepository;
import com.ulima.incidenciaurbana.repository.TecnicoRepository;
import com.ulima.incidenciaurbana.repository.UbicacionRepository;
import com.ulima.incidenciaurbana.repository.HistorialEstadoRepository;
import com.ulima.incidenciaurbana.service.IReporteService;
import com.ulima.incidenciaurbana.service.INotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReporteServiceImpl implements IReporteService {

    private final ReporteRepository reporteRepository;
    private final CuentaRepository cuentaRepository;
    private final TecnicoRepository tecnicoRepository;
    private final UbicacionRepository ubicacionRepository;
    private final HistorialEstadoRepository historialEstadoRepository;
    private final INotificationService notificationService;
    private final ReporteQueryServiceImpl queryService;

    @Autowired
    public ReporteServiceImpl(ReporteRepository reporteRepository,
            CuentaRepository cuentaRepository,
            TecnicoRepository tecnicoRepository,
            UbicacionRepository ubicacionRepository,
            HistorialEstadoRepository historialEstadoRepository,
            INotificationService notificationService,
            ReporteQueryServiceImpl queryService) {
        this.reporteRepository = reporteRepository;
        this.cuentaRepository = cuentaRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.ubicacionRepository = ubicacionRepository;
        this.historialEstadoRepository = historialEstadoRepository;
        this.notificationService = notificationService;
        this.queryService = queryService;
    }

    @Override
    public ReporteDTO crearReporte(ReporteDTO reporteDTO) {
        if (reporteDTO.getUbicacion() == null)
            throw new RuntimeException("La ubicacion es obligatoria para crear un reporte");
        if (reporteDTO.getUbicacion().getLatitud() == null || reporteDTO.getUbicacion().getLongitud() == null)
            throw new RuntimeException("La latitud y longitud son obligatorias en la ubicacion");
        if (reporteDTO.getCuentaId() == null)
            throw new RuntimeException("El ID de la cuenta es obligatorio");

        Cuenta cuenta = cuentaRepository.findById(reporteDTO.getCuentaId())
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada con id: " + reporteDTO.getCuentaId()));

        Reporte reporte = new Reporte(reporteDTO.getTitulo(), reporteDTO.getDescripcion(), cuenta);

        if (reporteDTO.getTipoProblema() != null) {
            reporte.setTipoProblema(reporteDTO.getTipoProblema());
        }

        Ubicacion ubicacion = new Ubicacion(
                reporteDTO.getUbicacion().getLatitud(),
                reporteDTO.getUbicacion().getLongitud(),
                reporteDTO.getUbicacion().getDireccion());
        ubicacion = ubicacionRepository.save(ubicacion);
        reporte.setUbicacion(ubicacion);

        cuenta.crearReporte(reporte);
        reporte = reporteRepository.save(reporte);

        historialEstadoRepository.save(new HistorialEstado(reporte, null, EstadoReporte.PENDIENTE));

        return queryService.convertirADTO(reporte);
    }

    @Override
    public ReporteDTO actualizarReporte(Long id, ReporteDTO reporteDTO) {
        if (id == null) throw new RuntimeException("El ID del reporte es obligatorio");
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado con id: " + id));

        reporte.setTitulo(reporteDTO.getTitulo());
        reporte.setDescripcion(reporteDTO.getDescripcion());

        if (reporteDTO.getTipoProblema() != null) {
            reporte.setTipoProblema(reporteDTO.getTipoProblema());
        }

        if (reporteDTO.getUbicacion() == null)
            throw new RuntimeException("La ubicacion es obligatoria");
        if (reporteDTO.getUbicacion().getLatitud() == null || reporteDTO.getUbicacion().getLongitud() == null)
            throw new RuntimeException("La latitud y longitud son obligatorias en la ubicacion");

        Ubicacion ubicacion = reporte.getUbicacion();
        ubicacion.setLatitud(reporteDTO.getUbicacion().getLatitud());
        ubicacion.setLongitud(reporteDTO.getUbicacion().getLongitud());
        ubicacion.setDireccion(reporteDTO.getUbicacion().getDireccion());
        ubicacionRepository.save(ubicacion);

        reporte = reporteRepository.save(reporte);
        return queryService.convertirADTO(reporte);
    }

    @Override
    public ReporteDTO cambiarEstadoReporte(Long id, EstadoReporte nuevoEstado) {
        if (id == null) throw new RuntimeException("El ID del reporte es obligatorio");
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado con id: " + id));

        EstadoReporte estadoAnterior = reporte.getEstado();
        if (estadoAnterior == nuevoEstado) return queryService.convertirADTO(reporte);

        reporte.cambiarEstado(nuevoEstado);
        reporte = reporteRepository.save(reporte);

        historialEstadoRepository.save(new HistorialEstado(reporte, estadoAnterior, nuevoEstado));

        String mensaje = "El estado de tu reporte '" + reporte.getTitulo() + "' ha cambiado a " + nuevoEstado;
        notificationService.enviarNotificacion(reporte.getCuenta().getId(), "Actualizacion de Reporte", mensaje);

        return queryService.convertirADTO(reporte);
    }

    @Override
    public ReporteDTO rechazarReporte(Long id, String motivo) {
        if (id == null) throw new RuntimeException("El ID del reporte es obligatorio");
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado con id: " + id));

        EstadoReporte estadoAnterior = reporte.getEstado();
        if (estadoAnterior == EstadoReporte.RECHAZADO) return queryService.convertirADTO(reporte);

        reporte.cambiarEstado(EstadoReporte.RECHAZADO);
        reporte.setComentarioResolucion(motivo);
        reporte.setFechaCierre(java.time.LocalDateTime.now());
        reporte = reporteRepository.save(reporte);

        historialEstadoRepository.save(new HistorialEstado(reporte, estadoAnterior, EstadoReporte.RECHAZADO));

        String mensaje = "Tu reporte ha sido rechazado. Motivo: " + motivo;
        notificationService.enviarNotificacion(reporte.getCuenta().getId(), "Reporte Rechazado", mensaje);

        return queryService.convertirADTO(reporte);
    }

    @Override
    public ReporteDTO asignarTecnico(Long reporteId, Long tecnicoId) {
        Reporte reporte = reporteRepository.findById(reporteId)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado con id: " + reporteId));

        if (reporte.getEstado() != EstadoReporte.REVISION) {
            throw new RuntimeException(
                    "El reporte debe estar en estado REVISION para asignar tecnico. Estado actual: " + reporte.getEstado());
        }

        Tecnico tecnico = tecnicoRepository.findById(tecnicoId)
                .orElseThrow(() -> new RuntimeException("Tecnico no encontrado con id: " + tecnicoId));

        reporte.setTecnico(tecnico);
        reporte = reporteRepository.save(reporte);

        notificationService.enviarNotificacion(reporte.getCuenta().getId(),
                "Tecnico Asignado",
                "Se ha asignado un tecnico a tu reporte '" + reporte.getTitulo() + "'");

        return queryService.convertirADTO(reporte);
    }

    @Override
    public void eliminarReporte(Long id) {
        if (id == null) throw new RuntimeException("El ID del reporte es obligatorio");
        if (!reporteRepository.existsById(id))
            throw new RuntimeException("Reporte no encontrado con id: " + id);
        reporteRepository.deleteById(id);
    }
}
