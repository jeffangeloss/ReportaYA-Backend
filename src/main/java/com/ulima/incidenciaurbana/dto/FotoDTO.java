package com.ulima.incidenciaurbana.dto;

import com.ulima.incidenciaurbana.model.TipoFoto;
import java.time.LocalDateTime;

public class FotoDTO {

    private Long id;
    private Long reporteId;
    private String url;
    private TipoFoto tipo;
    private String descripcion;
    private LocalDateTime fechaCarga;

    public FotoDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReporteId() { return reporteId; }
    public void setReporteId(Long reporteId) { this.reporteId = reporteId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public TipoFoto getTipo() { return tipo; }
    public void setTipo(TipoFoto tipo) { this.tipo = tipo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(LocalDateTime fechaCarga) { this.fechaCarga = fechaCarga; }
}
