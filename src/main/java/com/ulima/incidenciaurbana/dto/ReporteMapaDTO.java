package com.ulima.incidenciaurbana.dto;

import com.ulima.incidenciaurbana.model.EstadoReporte;
import com.ulima.incidenciaurbana.model.TipoProblema;

/**
 * Version liviana de ReporteDTO para pintar pines en el mapa.
 * Evita enviar fotos, descripcion y demas campos que el mapa no usa.
 */
public class ReporteMapaDTO {
    private Long id;
    private String titulo;
    private EstadoReporte estado;
    private TipoProblema tipoProblema;
    private UbicacionDTO ubicacion;

    public ReporteMapaDTO() {}

    public ReporteMapaDTO(Long id, String titulo, EstadoReporte estado, TipoProblema tipoProblema, UbicacionDTO ubicacion) {
        this.id = id;
        this.titulo = titulo;
        this.estado = estado;
        this.tipoProblema = tipoProblema;
        this.ubicacion = ubicacion;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public EstadoReporte getEstado() { return estado; }
    public void setEstado(EstadoReporte estado) { this.estado = estado; }

    public TipoProblema getTipoProblema() { return tipoProblema; }
    public void setTipoProblema(TipoProblema tipoProblema) { this.tipoProblema = tipoProblema; }

    public UbicacionDTO getUbicacion() { return ubicacion; }
    public void setUbicacion(UbicacionDTO ubicacion) { this.ubicacion = ubicacion; }
}
